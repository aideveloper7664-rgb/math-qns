package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val status: String = "active",
    val rank: String = "Bronze",
    val mmr: Int = 1000,
    val xp: Int = 0,
    val walletBalance: Double = 0.0,
    val lockedBalance: Double = 0.0,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalWinnings: Double = 0.0,
    val totalDeposited: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val paidGameplayRestricted: Boolean = false,
    val verificationStatus: String = "unverified",
    val isVerified: Boolean = false,
    val hasGoldCrown: Boolean = false,
    val vipTier: String = "none",
    val vipExpiresAt: Long? = null,
    val referralCode: String = "SOLVER1",
    val referralCount: Int = 0,
    val referralEarnings: Double = 0.0,
    val chatBannedUntil: Long? = null,
    val lastSeenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val mode: String, // "1v1", "2v2", "4v4", "mega", "practice"
    val entryFee: Double,
    val prizePool: Double,
    val status: String, // "COMPLETED", "LIVE", "CANCELLED"
    val winnerId: String?,
    val questionCount: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "match_participants")
data class MatchParticipantEntity(
    @PrimaryKey val id: String,
    val matchId: String,
    val userId: String,
    val userName: String,
    val userPhoto: String? = null,
    val userRank: String = "Bronze",
    val score: Int = 0,
    val result: String = "LOSS", // "WIN", "LOSS"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // "deposit", "withdrawal", "match_win", "match_entry", "vip_purchase", "referral_bonus"
    val amount: Double,
    val status: String, // "COMPLETED", "PENDING", "FAILED"
    val gatewayOrAccount: String = "Instant Gateway",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val category: String = "Arithmetic",
    val difficulty: String = "Medium"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val roomSlug: String, // "general", "vip-lounge", "high-rollers", "strategy"
    val userId: String,
    val userName: String,
    val message: String,
    val isVerified: Boolean = false,
    val hasGoldCrown: Boolean = false,
    val vipTier: String = "none",
    val isMine: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val status: String, // "LIVE", "UPCOMING", "COMPLETED"
    val entryFee: Double,
    val prizePool: Double,
    val playersJoined: Int,
    val maxPlayers: Int,
    val questionCount: Int,
    val startTime: Long
)

@Entity(tableName = "knockout_tournaments")
data class KnockoutTournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String, // "REGISTRATION", "IN_PROGRESS", "COMPLETED"
    val entryFee: Double,
    val prizePool: Double,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val scheduledStart: Long
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val category: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val rarity: String, // "common", "rare", "epic", "legendary"
    val colorHex: String,
    val isOwned: Boolean = false
)
