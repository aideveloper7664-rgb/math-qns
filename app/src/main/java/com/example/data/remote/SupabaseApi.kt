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
        @Query("id") idFilter: String? = null
    ): Response<List<SupabaseUserDto>>

    @POST("rest/v1/users")
    suspend fun insertUser(
        @Header("Prefer") prefer: String = "return=representation",
        @Body user: SupabaseUserDto
    ): Response<List<SupabaseUserDto>>

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

    @POST("rest/v1/match_participants")
    suspend fun postMatchParticipant(
        @Body participant: SupabaseMatchParticipantDto
    ): Response<Unit>

    @GET("rest/v1/app_settings")
    suspend fun getAppSettings(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseSettingDto>>
}
