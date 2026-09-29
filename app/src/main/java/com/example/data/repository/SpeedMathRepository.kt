package com.example.data.repository

import com.example.data.local.SpeedMathDao
import com.example.data.model.*
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

class SpeedMathRepository(private val dao: SpeedMathDao) {

    val topUsersByMmr: Flow<List<UserEntity>> = dao.getTopUsersByMmr()
    val topUsersByXp: Flow<List<UserEntity>> = dao.getTopUsersByXp()
    val topUsersByWins: Flow<List<UserEntity>> = dao.getTopUsersByWins()
    val allTournaments: Flow<List<TournamentEntity>> = dao.getAllTournamentsFlow()
    val allKnockoutTournaments: Flow<List<KnockoutTournamentEntity>> = dao.getAllKnockoutTournamentsFlow()
    val notifications: Flow<List<NotificationEntity>> = dao.getNotificationsFlow()
    val unreadNotificationsCount: Flow<Int> = dao.getUnreadNotificationsCountFlow()
    val allBadges: Flow<List<BadgeEntity>> = dao.getAllBadgesFlow()

    fun getUserFlow(userId: String): Flow<UserEntity?> = dao.getUserFlow(userId)

    suspend fun getUserById(userId: String): UserEntity? = dao.getUserById(userId)

    fun getUserMatchParticipants(userId: String): Flow<List<MatchParticipantEntity>> =
        dao.getUserMatchParticipantsFlow(userId)

    fun getUserTransactions(userId: String): Flow<List<TransactionEntity>> =
        dao.getUserTransactionsFlow(userId)

    fun getChatMessages(roomSlug: String): Flow<List<ChatMessageEntity>> =
        dao.getChatMessagesFlow(roomSlug)

    suspend fun saveUser(user: UserEntity) = dao.insertUser(user)

    suspend fun updateUser(user: UserEntity) = dao.updateUser(user)

    suspend fun markAllNotificationsRead() = dao.markAllNotificationsRead()

    // -------------------------------------------------------------
    // Supabase Auth Integration
    // -------------------------------------------------------------

    suspend fun supabaseSignIn(email: String, pass: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.authApi.signIn(SupabaseAuthRequest(email, pass))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val token = body.accessToken
                val userId = body.user?.id ?: return@withContext null
                val userEmail = body.user.email ?: email
                val userDisplayName = body.user.userMetadata?.get("display_name")?.toString() ?: userEmail.substringBefore("@")

                if (!token.isNullOrBlank()) {
                    com.example.data.local.SessionManager.saveSession(
                        accessToken = token,
                        refreshToken = body.refreshToken,
                        userId = userId,
                        email = userEmail,
                        displayName = userDisplayName
                    )
                }
                SupabaseClient.authToken = token
                SupabaseClient.currentUserId = userId

                // Ensure user profile row exists in public.users (auto-creates if missing)
                return@withContext ensureUserProfileExists(userId, userEmail, userDisplayName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun supabaseSignUp(email: String, pass: String, username: String, refCode: String?): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val req = SupabaseAuthRequest(email, pass, mapOf("display_name" to username))
            val response = SupabaseClient.authApi.signUp(req)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                var token = body.accessToken
                val userId = body.user?.id ?: return@withContext null

                // If accessToken wasn't returned directly on signup, sign in immediately
                if (token.isNullOrBlank()) {
                    val signInRes = SupabaseClient.authApi.signIn(SupabaseAuthRequest(email, pass))
                    if (signInRes.isSuccessful && signInRes.body() != null) {
                        token = signInRes.body()!!.accessToken
                    }
                }

                if (!token.isNullOrBlank()) {
                    com.example.data.local.SessionManager.saveSession(
                        accessToken = token,
                        refreshToken = body.refreshToken,
                        userId = userId,
                        email = email,
                        displayName = username
                    )
                }
                SupabaseClient.authToken = token
                SupabaseClient.currentUserId = userId

                // Check referral code if provided
                var referrerId: String? = null
                if (!refCode.isNullOrBlank()) {
                    try {
                        val refRes = SupabaseClient.restApi.getUsers(refCodeFilter = "eq.${refCode.trim().uppercase()}")
                        val referrer = refRes.body()?.firstOrNull()
                        if (referrer != null && referrer.id != userId) {
                            referrerId = referrer.id
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("SIGNUP", "Referral lookup failed", e)
                    }
                }

                val myRefCode = "REF" + (100000..999999).random()

                // STEP 3: Insert row into public.users (CRITICAL!)
                val newDto = SupabaseUserDto(
                    id = userId,
                    email = email,
                    displayName = username,
                    status = "active",
                    rank = "Bronze",
                    mmr = 1000,
                    xp = 0,
                    walletBalance = 0.0,
                    lockedBalance = 0.0,
                    matchesPlayed = 0,
                    wins = 0,
                    losses = 0,
                    totalWinnings = 0.0,
                    totalDeposited = 0.0,
                    totalWithdrawn = 0.0,
                    paidGameplayRestricted = false,
                    verificationStatus = "unverified",
                    referralCode = myRefCode
                )

                try {
                    val insertRes = SupabaseClient.restApi.insertUser(user = newDto)
                    android.util.Log.d("SIGNUP", "✅ Profile row created in users table: code=${insertRes.code()}")

                    if (referrerId != null) {
                        try {
                            SupabaseClient.restApi.updateUser(
                                idQuery = "eq.$userId",
                                updates = mapOf("referred_by" to referrerId)
                            )
                            SupabaseClient.restApi.insertReferral(
                                mapOf(
                                    "referrer_id" to referrerId,
                                    "referred_id" to userId,
                                    "referral_code" to (refCode?.trim()?.uppercase() ?: ""),
                                    "reward_amount" to 50.0,
                                    "reward_paid" to false,
                                    "referred_name" to username
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("SIGNUP", "Failed to insert referral row", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SIGNUP", "❌ Signup profile creation failed", e)
                }


                val entity = mapDtoToUserEntity(newDto)
                dao.insertUser(entity)
                return@withContext entity
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun ensureUserProfileExists(userId: String, email: String, displayName: String?): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.restApi.getUsers(idFilter = "eq.$userId")
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val dto = res.body()!!.first()
                val entity = mapDtoToUserEntity(dto)
                dao.insertUser(entity)
                return@withContext entity
            }

            // Profile row is missing from public.users! Create it now
            android.util.Log.d("AUTH", "Profile missing from public.users table — auto-creating for $userId")
            val newDto = SupabaseUserDto(
                id = userId,
                email = email,
                displayName = displayName ?: email.substringBefore("@"),
                status = "active",
                rank = "Bronze",
                mmr = 1000,
                xp = 0,
                walletBalance = 0.0,
                lockedBalance = 0.0,
                matchesPlayed = 0,
                wins = 0,
                losses = 0,
                totalWinnings = 0.0,
                totalDeposited = 0.0,
                totalWithdrawn = 0.0,
                paidGameplayRestricted = false,
                verificationStatus = "unverified",
                referralCode = "REF" + Random.nextInt(1000, 9999)
            )

            try {
                val insertRes = SupabaseClient.restApi.insertUser(user = newDto)
                android.util.Log.d("AUTH", "Profile auto-create response: ${insertRes.code()}")
                if (!insertRes.isSuccessful) {
                    val basicUser = mapOf(
                        "id" to userId,
                        "email" to email,
                        "display_name" to (displayName ?: email.substringBefore("@")),
                        "status" to "active",
                        "rank" to "Bronze",
                        "mmr" to 1000,
                        "xp" to 0,
                        "wallet_balance" to 0.0
                    )
                    SupabaseClient.restApi.insertUserMap(
                        prefer = "resolution=merge-duplicates,return=representation",
                        user = basicUser
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AUTH", "Failed to auto-create profile with DTO, trying map fallback", e)
                try {
                    val basicUser = mapOf(
                        "id" to userId,
                        "email" to email,
                        "display_name" to (displayName ?: email.substringBefore("@")),
                        "status" to "active",
                        "rank" to "Bronze",
                        "mmr" to 1000,
                        "xp" to 0,
                        "wallet_balance" to 0.0
                    )
                    SupabaseClient.restApi.insertUserMap(
                        prefer = "resolution=merge-duplicates,return=representation",
                        user = basicUser
                    )
                } catch (e2: Exception) {
                    android.util.Log.e("AUTH", "Map fallback also failed", e2)
                }
            }

            val entity = mapDtoToUserEntity(newDto)
            dao.insertUser(entity)
            return@withContext entity
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext dao.getUserById(userId)
    }

    suspend fun supabaseResetPassword(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.authApi.resetPassword(mapOf("email" to email))
            return@withContext res.isSuccessful
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun updateLocalBalance(userId: String, newBalance: Double) = withContext(Dispatchers.IO) {
        try {
            val user = dao.getUserById(userId)
            if (user != null) {
                dao.insertUser(user.copy(walletBalance = newBalance))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUserProfile(userId: String, displayName: String, photoUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = mutableMapOf<String, Any?>("display_name" to displayName)
            if (photoUrl != null) updates["photo_url"] = photoUrl
            val res = SupabaseClient.restApi.updateUser("eq.$userId", updates = updates)
            if (res.isSuccessful && res.body() != null) {
                res.body()!!.firstOrNull()?.let { dto ->
                    dao.insertUser(mapDtoToUserEntity(dto))
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }



    // -------------------------------------------------------------
    // Sync Real Data from Supabase
    // -------------------------------------------------------------

    suspend fun syncAllRealDataFromSupabase(currentUserId: String?) = withContext(Dispatchers.IO) {
        try {
            // 1. Sync Current User Profile
            if (currentUserId != null) {
                fetchAndSyncUserProfile(currentUserId, null)
            }

            // 2. Sync Top Users / Leaderboard
            val usersRes = SupabaseClient.restApi.getUsers(limit = 50)
            if (usersRes.isSuccessful && usersRes.body() != null) {
                val userEntities = usersRes.body()!!.map { mapDtoToUserEntity(it) }
                userEntities.forEach { dao.insertUser(it) }
            }

            // 3. Sync Questions
            val questionsRes = SupabaseClient.restApi.getQuestions(limit = 100)
            if (questionsRes.isSuccessful && questionsRes.body() != null) {
                val qEntities = questionsRes.body()!!.map { dto ->
                    QuestionEntity(
                        id = dto.id,
                        text = dto.text ?: dto.question ?: "Math Question",
                        optionA = dto.optionA ?: "A",
                        optionB = dto.optionB ?: "B",
                        optionC = dto.optionC ?: "C",
                        optionD = dto.optionD ?: "D",
                        correctAnswer = dto.correctAnswer ?: "A",
                        category = dto.category ?: "Arithmetic",
                        difficulty = dto.difficulty ?: "Medium"
                    )
                }
                dao.insertQuestions(qEntities)
            }

            // 4. Sync Tournaments
            val tourRes = SupabaseClient.restApi.getTournaments()
            if (tourRes.isSuccessful && tourRes.body() != null) {
                val tourEntities = tourRes.body()!!.map { dto ->
                    TournamentEntity(
                        id = dto.id,
                        name = dto.name ?: "Tournament",
                        description = dto.description ?: "Arena Tournament",
                        status = dto.status ?: "UPCOMING",
                        entryFee = dto.entryFee ?: 0.0,
                        prizePool = dto.prizePool ?: 0.0,
                        playersJoined = dto.playersJoined ?: 0,
                        maxPlayers = dto.maxPlayers ?: 100,
                        questionCount = dto.questionCount ?: 10,
                        startTime = parseDateToLong(dto.startTime)
                    )
                }
                dao.insertTournaments(tourEntities)
            }

            // 5. Sync Knockout Tournaments
            val koRes = SupabaseClient.restApi.getKnockoutTournaments()
            if (koRes.isSuccessful && koRes.body() != null) {
                val koEntities = koRes.body()!!.map { dto ->
                    KnockoutTournamentEntity(
                        id = dto.id,
                        name = dto.name ?: "Knockout Championship",
                        status = dto.status ?: "REGISTRATION",
                        entryFee = dto.entryFee ?: 0.0,
                        prizePool = dto.prizePool ?: 0.0,
                        currentPlayers = dto.currentPlayers ?: 0,
                        maxPlayers = dto.maxPlayers ?: 16,
                        scheduledStart = parseDateToLong(dto.scheduledStart)
                    )
                }
                dao.insertKnockoutTournaments(koEntities)
            }

            // 6. Sync Notifications & Announcements
            val notifRes = SupabaseClient.restApi.getNotifications()
            if (notifRes.isSuccessful && notifRes.body() != null) {
                val notifEntities = notifRes.body()!!.map { dto ->
                    NotificationEntity(
                        id = dto.id,
                        title = dto.title ?: "Update",
                        message = dto.message ?: "",
                        category = dto.category ?: "Announcement",
                        createdAt = parseDateToLong(dto.createdAt)
                    )
                }
                dao.insertNotifications(notifEntities)
            }

            // 7. Sync Badges
            val badgeRes = SupabaseClient.restApi.getBadges()
            if (badgeRes.isSuccessful && badgeRes.body() != null) {
                val badgeEntities = badgeRes.body()!!.map { dto ->
                    BadgeEntity(
                        id = dto.id,
                        name = dto.name,
                        icon = dto.icon ?: "🏅",
                        rarity = dto.rarity ?: "common",
                        colorHex = dto.color ?: "#2AD9C9"
                    )
                }
                dao.insertBadges(badgeEntities)
            }

            // 8. Sync User Badges
            if (currentUserId != null) {
                val userBadgeRes = SupabaseClient.restApi.getUserBadges("eq.$currentUserId")
                if (userBadgeRes.isSuccessful && userBadgeRes.body() != null) {
                    userBadgeRes.body()!!.forEach { ub ->
                        dao.unlockBadge(ub.badgeId)
                    }
                }
            }

            // 9. Sync User Transactions & Withdrawals
            if (currentUserId != null) {
                val txRes = SupabaseClient.restApi.getTransactions("eq.$currentUserId")
                if (txRes.isSuccessful && txRes.body() != null) {
                    val txEntities = txRes.body()!!.map { dto ->
                        TransactionEntity(
                            id = dto.id ?: UUID.randomUUID().toString(),
                            userId = dto.userId,
                            type = dto.type,
                            amount = dto.amount,
                            status = dto.status,
                            createdAt = parseDateToLong(dto.createdAt)
                        )
                    }
                    txEntities.forEach { dao.insertTransaction(it) }
                }

                syncUserWithdrawals(currentUserId)
            }

            // 10. Sync Recent Chat Messages
            syncChatRoomMessages("general")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncChatRoomMessages(roomSlug: String) = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.restApi.getChatMessages(roomQuery = "eq.$roomSlug")
            if (res.isSuccessful && res.body() != null) {
                val chatEntities = res.body()!!.map { dto ->
                    ChatMessageEntity(
                        id = dto.id ?: UUID.randomUUID().toString(),
                        roomSlug = dto.roomSlug,
                        userId = dto.userId,
                        userName = dto.userName,
                        message = dto.message,
                        isMine = false,
                        createdAt = parseDateToLong(dto.createdAt)
                    )
                }
                chatEntities.forEach { dao.insertChatMessage(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchAndSyncUserProfile(userId: String, emailFallback: String?): UserEntity? {
        try {
            val res = SupabaseClient.restApi.getUsers(idFilter = "eq.$userId")
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val dto = res.body()!!.first()
                val entity = mapDtoToUserEntity(dto)
                dao.insertUser(entity)
                return entity
            } else if (emailFallback != null) {
                val newDto = SupabaseUserDto(
                    id = userId,
                    email = emailFallback,
                    displayName = emailFallback.split("@")[0],
                    walletBalance = 0.0,
                    referralCode = "REF" + Random.nextInt(1000, 9999)
                )
                try {
                    SupabaseClient.restApi.insertUser(user = newDto)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val entity = mapDtoToUserEntity(newDto)
                dao.insertUser(entity)
                return entity
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dao.getUserById(userId)
    }

    private fun mapDtoToUserEntity(dto: SupabaseUserDto): UserEntity {
        return UserEntity(
            id = dto.id,
            email = dto.email ?: "user@speedmath.arena",
            displayName = dto.displayName ?: "Solver",
            photoUrl = dto.photoUrl,
            status = dto.status ?: "active",
            rank = dto.rank ?: "Bronze",
            mmr = dto.mmr ?: 1000,
            xp = dto.xp ?: 0,
            walletBalance = dto.walletBalance ?: 0.0,
            lockedBalance = dto.lockedBalance ?: 0.0,
            matchesPlayed = dto.matchesPlayed ?: 0,
            wins = dto.wins ?: 0,
            losses = dto.losses ?: 0,
            totalWinnings = dto.totalWinnings ?: 0.0,
            totalDeposited = dto.totalDeposited ?: 0.0,
            totalWithdrawn = dto.totalWithdrawn ?: 0.0,
            paidGameplayRestricted = dto.paidGameplayRestricted ?: false,
            verificationStatus = dto.verificationStatus ?: "unverified",
            isVerified = dto.isVerified ?: false,
            hasGoldCrown = dto.hasGoldCrown ?: false,
            vipTier = dto.vipTier ?: "none",
            vipExpiresAt = parseDateToLong(dto.vipExpiresAt),
            referralCode = dto.referralCode ?: "REF100",
            referralCount = dto.referralCount ?: 0,
            referralEarnings = dto.referralEarnings ?: 0.0
        )
    }

    private fun parseDateToLong(dateStr: String?): Long {
        if (dateStr.isNull_or_Empty()) return System.currentTimeMillis()
        return try {
            java.time.Instant.parse(dateStr).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun String?.isNull_or_Empty(): Boolean = this == null || this.isEmpty()

    // -------------------------------------------------------------
    // Real Actions with Supabase Sync
    // -------------------------------------------------------------

    suspend fun sendChatMessage(roomSlug: String, userId: String, userName: String, messageText: String) = withContext(Dispatchers.IO) {
        val dto = SupabaseChatMessageDto(
            roomSlug = roomSlug,
            userId = userId,
            userName = userName,
            message = messageText
        )
        try {
            val res = SupabaseClient.restApi.postChatMessage(message = dto)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val created = res.body()!!.first()
                val chat = ChatMessageEntity(
                    id = created.id ?: UUID.randomUUID().toString(),
                    roomSlug = created.roomSlug,
                    userId = created.userId,
                    userName = created.userName,
                    message = created.message,
                    isMine = true,
                    createdAt = parseDateToLong(created.createdAt)
                )
                dao.insertChatMessage(chat)
                return@withContext
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Local fallback if offline
        val chat = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            roomSlug = roomSlug,
            userId = userId,
            userName = userName,
            message = messageText,
            isMine = true
        )
        dao.insertChatMessage(chat)
    }

    suspend fun depositMoney(userId: String, amount: Double) = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext
        val newBalance = user.walletBalance + amount
        val newDeposited = user.totalDeposited + amount

        val updated = user.copy(
            walletBalance = newBalance,
            totalDeposited = newDeposited
        )
        dao.updateUser(updated)

        // Post to Supabase REST
        try {
            SupabaseClient.restApi.insertDepositRow(
                deposit = mapOf(
                    "user_id" to userId,
                    "amount" to amount,
                    "gateway" to "zapupi",
                    "status" to "PENDING"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = "deposit",
            amount = amount,
            status = "COMPLETED",
            gatewayOrAccount = "ZapUPI Instant"
        )
        dao.insertTransaction(tx)

        val notif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = "Deposit Successful",
            message = "₹${amount.toInt()} has been credited to your account balance.",
            category = "Wallet"
        )
        dao.insertNotification(notif)
    }

    suspend fun withdrawMoney(
        userId: String,
        amount: Double,
        method: String,
        account: String,
        accountHolderName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext false
        if (user.walletBalance < amount) return@withContext false

        // DO NOT update users.wallet_balance directly on pending withdrawal request.
        // Only insert the pending record into withdrawals table & transactions.
        try {
            val withdrawalDto = SupabaseWithdrawalDto(
                userId = userId,
                userName = user.displayName,
                userEmail = user.email,
                amount = amount,
                method = method,
                upiId = if (method.equals("UPI", ignoreCase = true)) account else null,
                accountHolderName = accountHolderName ?: user.displayName,
                reference = account,
                status = "PENDING"
            )
            SupabaseClient.restApi.postWithdrawal(withdrawalDto)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = "withdrawal",
            amount = amount,
            status = "PENDING",
            gatewayOrAccount = "$method ($account)"
        )
        dao.insertTransaction(tx)

        val notif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = "Withdrawal Requested",
            message = "₹${amount.toInt()} withdrawal via $method submitted for processing.",
            category = "Wallet"
        )
        dao.insertNotification(notif)
        return@withContext true
    }

    suspend fun syncUserWithdrawals(userId: String) = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.restApi.getWithdrawals("eq.$userId")
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                for (w in list) {
                    val status = (w.status ?: "PENDING").uppercase()
                    if (status == "APPROVED" || status == "COMPLETED" || status == "SUCCESS") {
                        dao.updateAllWithdrawalStatus(userId, "APPROVED")
                        val notifId = "withdrawn_approved_${w.id ?: userId}"
                        val notif = NotificationEntity(
                            id = notifId,
                            title = "Withdrawal Approved! 🎉",
                            message = "Your withdrawal of ₹${w.amount.toInt()} has been APPROVED and successfully sent.",
                            category = "Wallet"
                        )
                        dao.insertNotification(notif)
                    } else if (status == "REJECTED" || status == "FAILED") {
                        dao.updateAllWithdrawalStatus(userId, "REJECTED")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getUserWithdrawalsFlow(userId: String) = dao.getUserWithdrawalsFlow(userId)

    suspend fun getUserReferrals(userId: String): List<SupabaseReferralDto> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.restApi.getReferrals("eq.$userId")
            if (res.isSuccessful && res.body() != null) {
                return@withContext res.body()!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }


    suspend fun purchaseVipPass(userId: String, tier: String, price: Double, durationDays: Long): Boolean = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext false
        if (user.walletBalance < price) return@withContext false

        val now = System.currentTimeMillis()
        val expires = now + (durationDays * 24 * 60 * 60 * 1000L)
        val newBalance = user.walletBalance - price

        val updated = user.copy(
            walletBalance = newBalance,
            vipTier = tier,
            vipExpiresAt = expires,
            hasGoldCrown = true
        )
        dao.updateUser(updated)

        try {
            SupabaseClient.restApi.updateUser("eq.$userId", updates = mapOf(
                "wallet_balance" to newBalance,
                "vip_tier" to tier,
                "has_gold_crown" to true
            ))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = "vip_purchase",
            amount = price,
            status = "COMPLETED",
            gatewayOrAccount = "VIP Pass $tier"
        )
        dao.insertTransaction(tx)

        dao.unlockBadge("vip_master")
        return@withContext true
    }

    suspend fun recordMatchResults(
        userId: String,
        mode: String,
        entryFee: Double,
        score: Int,
        correctAnswers: Int,
        totalQuestions: Int,
        isPractice: Boolean
    ): MatchParticipantEntity = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: UserEntity("default", "user@example.com", "ProSolver")
        val matchId = UUID.randomUUID().toString()

        val won = !isPractice && (correctAnswers.toDouble() / totalQuestions) >= 0.6
        val prize = if (won) entryFee * 1.8 else 0.0

        val participant = MatchParticipantEntity(
            id = UUID.randomUUID().toString(),
            matchId = matchId,
            userId = userId,
            userName = user.displayName,
            userPhoto = user.photoUrl,
            userRank = user.rank,
            score = score,
            result = if (won) "WIN" else if (isPractice) "COMPLETED" else "LOSS"
        )
        dao.insertMatchParticipant(participant)

        try {
            SupabaseClient.restApi.postMatchParticipant(
                SupabaseMatchParticipantDto(
                    matchId = matchId,
                    userId = userId,
                    score = score,
                    result = if (won) "WIN" else "LOSS"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!isPractice) {
            val mmrDelta = if (won) 25 else -15
            val xpGained = score / 2 + 50
            val newMmr = maxOf(100, user.mmr + mmrDelta)
            val newRank = calculateRank(newMmr)
            val newWins = if (won) user.wins + 1 else user.wins
            val newLosses = if (!won) user.losses + 1 else user.losses
            val newMatches = user.matchesPlayed + 1
            val newWinnings = user.totalWinnings + prize
            val newWallet = maxOf(0.0, user.walletBalance + prize - entryFee)

            val updatedUser = user.copy(
                mmr = newMmr,
                rank = newRank,
                xp = user.xp + xpGained,
                matchesPlayed = newMatches,
                wins = newWins,
                losses = newLosses,
                totalWinnings = newWinnings,
                walletBalance = newWallet
            )
            dao.updateUser(updatedUser)

            try {
                SupabaseClient.restApi.updateUser("eq.$userId", updates = mapOf(
                    "mmr" to newMmr,
                    "rank" to newRank,
                    "xp" to user.xp + xpGained,
                    "matches_played" to newMatches,
                    "wins" to newWins,
                    "losses" to newLosses,
                    "total_winnings" to newWinnings,
                    "wallet_balance" to newWallet
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext participant
    }

    private fun calculateRank(mmr: Int): String {
        return when {
            mmr >= 2500 -> "Legend"
            mmr >= 2200 -> "Grandmaster"
            mmr >= 1900 -> "Master"
            mmr >= 1600 -> "Diamond"
            mmr >= 1400 -> "Platinum"
            mmr >= 1200 -> "Gold"
            mmr >= 1050 -> "Silver"
            else -> "Bronze"
        }
    }

    suspend fun getQuestionsForGame(count: Int): List<QuestionEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getAllQuestions()
        val list = mutableListOf<QuestionEntity>()
        list.addAll(existing.shuffled().take(count))

        while (list.size < count) {
            list.add(generateRandomMathQuestion())
        }
        return@withContext list.take(count)
    }

    private fun generateRandomMathQuestion(): QuestionEntity {
        val opType = Random.nextInt(0, 5)
        var qText = ""
        var answer = 0

        when (opType) {
            0 -> {
                val a = Random.nextInt(12, 180)
                val b = Random.nextInt(12, 180)
                qText = "What is $a + $b ?"
                answer = a + b
            }
            1 -> {
                val a = Random.nextInt(50, 300)
                val b = Random.nextInt(12, a)
                qText = "What is $a - $b ?"
                answer = a - b
            }
            2 -> {
                val a = Random.nextInt(6, 25)
                val b = Random.nextInt(6, 25)
                qText = "Calculate $a × $b"
                answer = a * b
            }
            3 -> {
                val b = Random.nextInt(3, 16)
                val ans = Random.nextInt(5, 25)
                val a = b * ans
                qText = "What is $a ÷ $b ?"
                answer = ans
            }
            else -> {
                val a = Random.nextInt(11, 30)
                qText = "What is $a² ?"
                answer = a * a
            }
        }

        val correctIndex = Random.nextInt(0, 4)
        val correctLetter = listOf("A", "B", "C", "D")[correctIndex]
        val options = mutableSetOf<Int>()
        options.add(answer)

        while (options.size < 4) {
            val offset = Random.nextInt(-15, 16)
            if (offset != 0 && answer + offset > 0) {
                options.add(answer + offset)
            }
        }

        val optList = options.shuffled().map { it.toString() }.toMutableList()
        val correctStr = answer.toString()
        val currPos = optList.indexOf(correctStr)
        if (currPos != -1 && currPos != correctIndex) {
            val temp = optList[correctIndex]
            optList[correctIndex] = correctStr
            optList[currPos] = temp
        }

        return QuestionEntity(
            id = UUID.randomUUID().toString(),
            text = qText,
            optionA = optList.getOrElse(0) { (answer + 1).toString() },
            optionB = optList.getOrElse(1) { (answer - 2).toString() },
            optionC = optList.getOrElse(2) { (answer + 5).toString() },
            optionD = optList.getOrElse(3) { (answer - 7).toString() },
            correctAnswer = correctLetter,
            category = "Speed Math",
            difficulty = if (answer > 100) "Hard" else "Medium"
        )
    }
}
