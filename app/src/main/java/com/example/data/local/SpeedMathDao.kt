package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedMathDao {

    // User Operations
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY mmr DESC LIMIT 50")
    fun getTopUsersByMmr(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY xp DESC LIMIT 50")
    fun getTopUsersByXp(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY wins DESC LIMIT 50")
    fun getTopUsersByWins(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // Matches
    @Query("SELECT * FROM match_participants WHERE userId = :userId ORDER BY timestamp DESC LIMIT 50")
    fun getUserMatchParticipantsFlow(userId: String): Flow<List<MatchParticipantEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchParticipant(participant: MatchParticipantEntity)

    // Transactions
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC LIMIT 100")
    fun getUserTransactionsFlow(userId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Questions
    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    // Chat
    @Query("SELECT * FROM chat_messages WHERE roomSlug = :roomSlug ORDER BY createdAt ASC LIMIT 100")
    fun getChatMessagesFlow(roomSlug: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    // Tournaments
    @Query("SELECT * FROM tournaments ORDER BY startTime ASC")
    fun getAllTournamentsFlow(): Flow<List<TournamentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournaments(tournaments: List<TournamentEntity>)

    // Knockout Tournaments
    @Query("SELECT * FROM knockout_tournaments ORDER BY scheduledStart ASC")
    fun getAllKnockoutTournamentsFlow(): Flow<List<KnockoutTournamentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnockoutTournaments(tournaments: List<KnockoutTournamentEntity>)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 50")
    fun getNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationsCountFlow(): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllNotificationsRead()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    // Badges
    @Query("SELECT * FROM badges")
    fun getAllBadgesFlow(): Flow<List<BadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<BadgeEntity>)

    @Query("UPDATE badges SET isOwned = 1 WHERE id = :badgeId")
    suspend fun unlockBadge(badgeId: String)
}
