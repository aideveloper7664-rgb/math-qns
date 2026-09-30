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
    @Json(name = "refresh_token") val refreshToken: String? = null,
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
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "user_email") val userEmail: String? = null,
    @Json(name = "amount") val amount: Double,
    @Json(name = "method") val method: String? = "UPI",
    @Json(name = "upi_id") val upiId: String? = null,
    @Json(name = "account_holder_name") val accountHolderName: String? = null,
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "status") val status: String? = "PENDING",
    @Json(name = "requested_at") val requestedAt: String? = null,
    @Json(name = "processed_at") val processedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseReferralDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "referrer_id") val referrerId: String,
    @Json(name = "referred_id") val referredId: String? = null,
    @Json(name = "referral_code") val referralCode: String,
    @Json(name = "reward_amount") val rewardAmount: Double = 0.0,
    @Json(name = "reward_paid") val rewardPaid: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "referred_name") val referredName: String? = null
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
    @Json(name = "benefits") val benefits: Any? = null,
    @Json(name = "enabled") val enabled: Boolean? = true
) {
    fun benefitLines(): List<String> {
        if (benefits is List<*>) {
            return benefits.mapNotNull { it?.toString() }
        }
        if (benefits is Map<*, *>) {
            return benefits.map { (k, v) ->
                val keyStr = k.toString().replace("_", " ").replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() 
                }
                "$keyStr: $v"
            }
        }
        return emptyList()
    }
}

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

// ─── Single-Player Progressive Game Models ──────────────────────────────────────

@JsonClass(generateAdapter = true)
data class QuestionData(
    @Json(name = "question_id") val questionId: String,
    @Json(name = "question_number") val questionNumber: Int,
    @Json(name = "question_text") val questionText: String,
    @Json(name = "option_a") val optionA: String,
    @Json(name = "option_b") val optionB: String,
    @Json(name = "option_c") val optionC: String,
    @Json(name = "option_d") val optionD: String,
    @Json(name = "difficulty") val difficulty: String = "easy",
    @Json(name = "time_limit_ms") val timeLimitMs: Long = 15000L,
    @Json(name = "correct_answer") val correctAnswer: String? = null
)

@JsonClass(generateAdapter = true)
data class StartGameResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "entry_fee") val entryFee: Double = 10.0,
    @Json(name = "new_balance") val newBalance: Double? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SubmitAnswerResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "correct") val correct: Boolean = false,
    @Json(name = "correct_answer") val correctAnswer: String? = null,
    @Json(name = "points_earned") val pointsEarned: Int = 0,
    @Json(name = "total_score") val totalScore: Int = 0,
    @Json(name = "correct_answers") val correctAnswers: Int = 0,
    @Json(name = "game_over") val gameOver: Boolean = false,
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "prize") val prize: Double = 0.0,
    @Json(name = "refund") val refund: Double = 0.0,
    @Json(name = "refund_applied") val refundApplied: Boolean = false,
    @Json(name = "error") val error: String? = null
)

data class GameResult(
    val totalScore: Int,
    val correctAnswers: Int,
    val questionsAnswered: Int,
    val prize: Double,
    val reason: String?,
    val refund: Double = 0.0,
    val refundApplied: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GameConfigRow(
    @Json(name = "key") val key: String,
    @Json(name = "value") val value: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class GameHistoryItem(
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "entry_fee") val entryFee: Double = 0.0,
    @Json(name = "total_score") val totalScore: Int = 0,
    @Json(name = "correct_answers") val correctAnswers: Int = 0,
    @Json(name = "wrong_answers") val wrongAnswers: Int = 0,
    @Json(name = "timeouts") val timeouts: Int = 0,
    @Json(name = "questions_attempted") val questionsAttempted: Int = 0,
    @Json(name = "status") val status: String = "",
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "ended_at") val endedAt: String? = null,
    @Json(name = "wallet_impact") val walletImpact: Double = 0.0,
    @Json(name = "prize_earned") val prizeEarned: Double = 0.0,
    @Json(name = "refund_amount") val refundAmount: Double? = null
)

@JsonClass(generateAdapter = true)
data class PurchaseVipResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "tier") val tier: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "amount_paid") val amountPaid: Double? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class TournamentLiveDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "entry_fee") val entryFee: Double? = 0.0,
    @Json(name = "prize_pool") val prizePool: Double? = 0.0,
    @Json(name = "players_joined") val playersJoined: Int? = 0,
    @Json(name = "max_players") val maxPlayers: Int? = 0,
    @Json(name = "start_time") val startTime: String? = null,
    @Json(name = "end_time") val endTime: String? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class KnockoutMatchupDto(
    @Json(name = "id") val id: String,
    @Json(name = "tournament_id") val tournamentId: String? = null,
    @Json(name = "round_number") val roundNumber: Int = 1,
    @Json(name = "matchup_number") val matchupNumber: Int = 1,
    @Json(name = "player1_id") val player1Id: String? = null,
    @Json(name = "player2_id") val player2Id: String? = null,
    @Json(name = "player1_score") val player1Score: Int? = null,
    @Json(name = "player2_score") val player2Score: Int? = null,
    @Json(name = "winner_id") val winnerId: String? = null,
    @Json(name = "status") val status: String = "PENDING"
)

@JsonClass(generateAdapter = true)
data class KnockoutParticipantDto(
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "seed") val seed: Int? = 1,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class GameSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "entry_fee") val entryFee: Double = 10.0,
    @Json(name = "status") val status: String = "IN_PROGRESS",
    @Json(name = "current_question") val currentQuestion: Int = 1,
    @Json(name = "total_score") val totalScore: Int = 0,
    @Json(name = "correct_answers") val correctAnswers: Int = 0,
    @Json(name = "wrong_answers") val wrongAnswers: Int = 0,
    @Json(name = "timeouts") val timeouts: Int = 0,
    @Json(name = "highest_question") val highestQuestion: Int = 0,
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "ended_at") val endedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class LeaderboardRow(
    @Json(name = "rank") val rank: Int = 0,
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "photo_url") val photoUrl: String? = null,
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "has_gold_crown") val hasGoldCrown: Boolean = false,
    @Json(name = "vip_tier") val vipTier: String = "none",
    @Json(name = "value") val value: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class LeaderboardMe(
    @Json(name = "rank") val rank: Int = 0,
    @Json(name = "value") val value: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class LeaderboardResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "metric") val metric: String = "",
    @Json(name = "rows") val rows: List<LeaderboardRow> = emptyList(),
    @Json(name = "me") val me: LeaderboardMe? = null,
    @Json(name = "generated_at") val generatedAt: String? = null,
    @Json(name = "error") val error: String? = null
)

