package com.example.data.remote

import retrofit2.Response
import retrofit2.http.*

interface SupabaseAuthApi {
    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Body request: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/recover")
    suspend fun resetPassword(
        @Body body: Map<String, String>
    ): Response<Unit>
}

interface SupabaseRestApi {
    // Users
    @GET("rest/v1/users")
    suspend fun getUsers(
        @Query("select") select: String = "*",
        @Query("order") order: String? = "mmr.desc",
        @Query("limit") limit: Int? = 50,
        @Query("id") idFilter: String? = null,
        @Query("referral_code") refCodeFilter: String? = null
    ): Response<List<SupabaseUserDto>>

    @POST("rest/v1/users")
    suspend fun insertUser(
        @Header("Prefer") prefer: String = "return=representation",
        @Body user: SupabaseUserDto
    ): Response<List<SupabaseUserDto>>

    @POST("rest/v1/users")
    suspend fun insertUserMap(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body user: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<Map<String, Any?>>>

    @PATCH("rest/v1/users")
    suspend fun updateUser(
        @Query("id") idQuery: String, // e.g. "eq.123"
        @Header("Prefer") prefer: String = "return=representation",
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<SupabaseUserDto>>

    // Questions
    @GET("rest/v1/questions")
    suspend fun getQuestions(
        @Query("select") select: String = "*",
        @Query("limit") limit: Int = 100
    ): Response<List<SupabaseQuestionDto>>

    // Tournaments
    @GET("rest/v1/tournaments")
    suspend fun getTournaments(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseTournamentDto>>

    // Knockout Tournaments
    @GET("rest/v1/knockout_tournaments")
    suspend fun getKnockoutTournaments(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseKnockoutDto>>

    // Chat
    @GET("rest/v1/chat_messages")
    suspend fun getChatMessages(
        @Query("room_slug") roomQuery: String, // e.g. "eq.general"
        @Query("is_deleted") isDeleted: String = "eq.false",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 50
    ): Response<List<SupabaseChatMessageDto>>

    @POST("rest/v1/chat_messages")
    suspend fun postChatMessage(
        @Header("Prefer") prefer: String = "return=representation",
        @Body message: SupabaseChatMessageDto
    ): Response<List<SupabaseChatMessageDto>>

    // Notifications
    @GET("rest/v1/notifications")
    suspend fun getNotifications(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 50
    ): Response<List<SupabaseNotificationDto>>

    @GET("rest/v1/announcements")
    suspend fun getAnnouncements(
        @Query("select") select: String = "*",
        @Query("status") status: String = "eq.active"
    ): Response<List<SupabaseAnnouncementDto>>

    // VIP Plans
    @GET("rest/v1/vip_plans")
    suspend fun getVipPlans(
        @Query("select") select: String = "*",
        @Query("enabled") enabled: String = "eq.true"
    ): Response<List<SupabaseVipPlanDto>>

    // Badges
    @GET("rest/v1/badges")
    suspend fun getBadges(
        @Query("select") select: String = "*",
        @Query("enabled") enabled: String = "eq.true"
    ): Response<List<SupabaseBadgeDto>>

    @GET("rest/v1/user_badges")
    suspend fun getUserBadges(
        @Query("user_id") userQuery: String // e.g. "eq.123"
    ): Response<List<SupabaseUserBadgeDto>>

    // Transactions
    @GET("rest/v1/transactions")
    suspend fun getTransactions(
        @Query("user_id") userQuery: String, // e.g. "eq.123"
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 50
    ): Response<List<SupabaseTransactionDto>>

    @POST("rest/v1/deposits")
    suspend fun insertDepositRow(
        @Header("Prefer") prefer: String = "return=representation",
        @Body deposit: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<com.example.data.repository.DepositRow>>

    @GET("rest/v1/deposits")
    suspend fun getDepositStatus(
        @Query("id") idQuery: String,
        @Query("select") select: String = "id,status,amount,verified_at"
    ): Response<List<com.example.data.repository.DepositStatusRow>>

    @GET("rest/v1/users")
    suspend fun getWalletInfo(
        @Query("id") idQuery: String,
        @Query("select") select: String = "wallet_balance,locked_balance,total_deposited,total_winnings,total_withdrawn"
    ): Response<List<com.example.data.repository.WalletInfo>>

    @POST("rest/v1/withdrawals")
    suspend fun postWithdrawal(
        @Body withdrawal: SupabaseWithdrawalDto
    ): Response<Unit>

    @GET("rest/v1/withdrawals")
    suspend fun getWithdrawals(
        @Query("user_id") userQuery: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseWithdrawalDto>>

    @PATCH("rest/v1/withdrawals")
    suspend fun updateWithdrawal(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>

    @GET("rest/v1/referrals")
    suspend fun getReferrals(
        @Query("referrer_id") referrerQuery: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseReferralDto>>

    @POST("rest/v1/referrals")
    suspend fun insertReferral(
        @Body referral: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>


    @POST("rest/v1/match_participants")
    suspend fun postMatchParticipant(
        @Body participant: SupabaseMatchParticipantDto
    ): Response<Unit>

    @GET("rest/v1/app_settings")
    suspend fun getAppSettings(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseSettingDto>>

    // ─── Single-Player Progressive Game RPCs ────────────────────────────────
    @POST("rest/v1/rpc/start_game")
    suspend fun startGameRpc(
        @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()
    ): Response<Map<String, Any?>>

    @POST("rest/v1/rpc/get_next_question")
    suspend fun getNextQuestionRpc(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @POST("rest/v1/rpc/submit_answer")
    suspend fun submitAnswerRpc(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @POST("rest/v1/rpc/end_game")
    suspend fun endGameRpc(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @GET("rest/v1/game_sessions")
    suspend fun getGameSessions(
        @Query("user_id") userQuery: String,
        @Query("order") order: String = "started_at.desc",
        @Query("limit") limit: Int = 30
    ): Response<List<GameSessionDto>>

    @PATCH("rest/v1/game_sessions")
    suspend fun updateGameSession(
        @Query("id") idQuery: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<GameSessionDto>>

    @POST("rest/v1/game_session_questions")
    suspend fun insertSessionQuestion(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>
}

