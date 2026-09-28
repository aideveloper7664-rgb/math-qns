package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseAuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "data") val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "user") val user: SupabaseUserObject?
)

@JsonClass(generateAdapter = true)
data class SupabaseUserObject(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "photo_url") val photoUrl: String? = null,
    @Json(name = "status") val status: String? = "active",
    @Json(name = "rank") val rank: String? = "Bronze",
    @Json(name = "mmr") val mmr: Int? = 1000,
    @Json(name = "xp") val xp: Int? = 0,
    @Json(name = "wallet_balance") val walletBalance: Double? = 0.0,
    @Json(name = "locked_balance") val lockedBalance: Double? = 0.0,
    @Json(name = "matches_played") val matchesPlayed: Int? = 0,
    @Json(name = "wins") val wins: Int? = 0,
    @Json(name = "losses") val losses: Int? = 0,
    @Json(name = "total_winnings") val totalWinnings: Double? = 0.0,
    @Json(name = "total_deposited") val totalDeposited: Double? = 0.0,
    @Json(name = "total_withdrawn") val totalWithdrawn: Double? = 0.0,
    @Json(name = "paid_gameplay_restricted") val paidGameplayRestricted: Boolean? = false,
    @Json(name = "verification_status") val verificationStatus: String? = "unverified",
    @Json(name = "is_verified") val isVerified: Boolean? = false,
    @Json(name = "has_gold_crown") val hasGoldCrown: Boolean? = false,
    @Json(name = "vip_tier") val vipTier: String? = "none",
    @Json(name = "vip_expires_at") val vipExpiresAt: String? = null,
    @Json(name = "referral_code") val referralCode: String? = null,
    @Json(name = "referral_count") val referralCount: Int? = 0,
    @Json(name = "referral_earnings") val referralEarnings: Double? = 0.0,
    @Json(name = "chat_banned_until") val chatBannedUntil: String? = null,
    @Json(name = "last_seen_at") val lastSeenAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseQuestionDto(
    @Json(name = "id") val id: String,
    @Json(name = "text") val text: String? = null,
    @Json(name = "question") val question: String? = null,
    @Json(name = "option_a") val optionA: String? = null,
    @Json(name = "option_b") val optionB: String? = null,
    @Json(name = "option_c") val optionC: String? = null,
    @Json(name = "option_d") val optionD: String? = null,
    @Json(name = "correct_answer") val correctAnswer: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "difficulty") val difficulty: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseTournamentDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "entry_fee") val entryFee: Double? = 0.0,
    @Json(name = "prize_pool") val prizePool: Double? = 0.0,
    @Json(name = "players_joined") val playersJoined: Int? = 0,
    @Json(name = "max_players") val maxPlayers: Int? = 0,
    @Json(name = "question_count") val questionCount: Int? = 10,
    @Json(name = "start_time") val startTime: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseKnockoutDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "entry_fee") val entryFee: Double? = 0.0,
    @Json(name = "prize_pool") val prizePool: Double? = 0.0,
    @Json(name = "current_players") val currentPlayers: Int? = 0,
    @Json(name = "max_players") val maxPlayers: Int? = 0,
    @Json(name = "scheduled_start") val scheduledStart: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseChatMessageDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "room_slug") val roomSlug: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "message") val message: String,
    @Json(name = "message_type") val messageType: String? = "text",
    @Json(name = "is_deleted") val isDeleted: Boolean? = false,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseTransactionDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String,
    @Json(name = "type") val type: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseDepositDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "gateway") val gateway: String? = "zapupi",
    @Json(name = "status") val status: String? = "PENDING"
)

@JsonClass(generateAdapter = true)
data class SupabaseWithdrawalDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "method") val method: String? = "UPI",
    @Json(name = "status") val status: String? = "PENDING"
)

@JsonClass(generateAdapter = true)
data class SupabaseNotificationDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAnnouncementDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = "active",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseVipPlanDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "tier") val tier: String,
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: Double,
    @Json(name = "duration_days") val durationDays: Long,
    @Json(name = "perks") val perks: List<String>? = null,
    @Json(name = "enabled") val enabled: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class SupabaseBadgeDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "rarity") val rarity: String? = "common",
    @Json(name = "color") val color: String? = null,
    @Json(name = "enabled") val enabled: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class SupabaseUserBadgeDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String,
    @Json(name = "badge_id") val badgeId: String
)

@JsonClass(generateAdapter = true)
data class SupabaseMatchParticipantDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "match_id") val matchId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "score") val score: Int,
    @Json(name = "result") val result: String? = "LOSS",
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseSettingDto(
    @Json(name = "key") val key: String,
    @Json(name = "value") val value: String? = null
)

@JsonClass(generateAdapter = true)
data class ZapUpiOrderRequest(
    @Json(name = "zap_key") val zapKey: String,
    @Json(name = "order_id") val orderId: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "customer_mobile") val customerMobile: String,
    @Json(name = "remark") val remark: String,
    @Json(name = "success_url") val successUrl: String,
    @Json(name = "failed_url") val failedUrl: String,
    @Json(name = "timeout_url") val timeoutUrl: String,
    @Json(name = "webhook_url") val webhookUrl: String
)

@JsonClass(generateAdapter = true)
data class ZapUpiOrderData(
    @Json(name = "payment_url") val paymentUrl: String? = null,
    @Json(name = "order_id") val orderId: String? = null
)

@JsonClass(generateAdapter = true)
data class ZapUpiOrderResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "environment") val environment: String? = null,
    @Json(name = "txn_id") val txnId: String? = null,
    @Json(name = "payment_url") val paymentUrl: String? = null,
    @Json(name = "data") val data: ZapUpiOrderData? = null
)

@JsonClass(generateAdapter = true)
data class ZapUpiStatusRequest(
    @Json(name = "zap_key") val zapKey: String,
    @Json(name = "order_id") val orderId: String
)

@JsonClass(generateAdapter = true)
data class ZapUpiStatusResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "txn_id") val txnId: String? = null,
    @Json(name = "amount") val amount: String? = null
)

// ─── Matchmaking Models ───────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class JoinMatchmakingRequest(
    @Json(name = "game_mode") val gameMode: String,
    @Json(name = "entry_fee") val entryFee: Double
)

@JsonClass(generateAdapter = true)
data class JoinMatchmakingResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "matched") val matched: Boolean = false,
    @Json(name = "match_id") val matchId: String? = null,
    @Json(name = "queue_id") val queueId: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class CancelMatchmakingResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "refunded") val refunded: Boolean = false,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class MatchmakingQueueDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "status") val status: String,
    @Json(name = "match_id") val matchId: String? = null
)
