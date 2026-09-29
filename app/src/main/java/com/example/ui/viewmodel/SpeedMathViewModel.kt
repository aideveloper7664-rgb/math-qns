package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.GameResult
import com.example.data.remote.GameSessionDto
import com.example.data.remote.QuestionData
import com.example.data.remote.SupabaseClient
import com.example.data.repository.DepositRepository
import com.example.data.repository.RpcResult
import com.example.data.repository.SinglePlayerGameRepository
import com.example.data.repository.SpeedMathRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameState(
    val active: Boolean = false,
    val sessionId: String? = null,
    val entryFee: Double = 10.0,
    val currentQuestion: QuestionData? = null,
    val questionNumber: Int = 1,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val questionsAnswered: Int = 0,
    val timeLeftMs: Long = 10000L,
    val timeLimitMs: Long = 10000L,
    val isLocked: Boolean = false,
    val selectedOption: String? = null,
    val lastIsCorrect: Boolean? = null,
    val lastCorrectAnswer: String? = null,
    val lastPointsEarned: Int = 0,
    val isGameOver: Boolean = false,
    val gameResult: GameResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorRetryable: Boolean = true
)

sealed class ToastEvent {
    data class Show(val message: String, val isError: Boolean = false) : ToastEvent()
}

class SpeedMathViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SpeedMathRepository(AppDatabase.getDatabase(application).speedMathDao())
    private val singlePlayerRepo = SinglePlayerGameRepository()
    private val depositRepo = DepositRepository()

    private var timerJob: Job? = null
    private var questionStartTime: Long = 0

    private val _paymentUrlEvent = MutableSharedFlow<String>()
    val paymentUrlEvent: SharedFlow<String> = _paymentUrlEvent.asSharedFlow()

    val currentUserId = MutableStateFlow<String?>(null)
    val withdrawSubmitting = MutableStateFlow(false)
    private var watcherJob: Job? = null
    private val database = AppDatabase.getDatabase(application)

    val currentUser: StateFlow<UserEntity?> = currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getUserFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _recentSessions = MutableStateFlow<List<GameSessionDto>>(emptyList())
    val recentSessions: StateFlow<List<GameSessionDto>> = _recentSessions.asStateFlow()

    val userTransactions: StateFlow<List<TransactionEntity>> = currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getUserTransactions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topUsersByMmr = repository.topUsersByMmr
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topUsersByXp = repository.topUsersByXp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topUsersByWins = repository.topUsersByWins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTournaments = repository.allTournaments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKnockoutTournaments = repository.allKnockoutTournaments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifsCount = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allBadges = repository.allBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedChatRoom = MutableStateFlow("general")

    val currentChatMessages: StateFlow<List<ChatMessageEntity>> = selectedChatRoom.flatMapLatest { room ->
        repository.getChatMessages(room)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<ToastEvent>()
    val toastEvent: SharedFlow<ToastEvent> = _toastEvent.asSharedFlow()

    init {
        val sessionUserId = com.example.data.local.SessionManager.userId
        if (sessionUserId != null) {
            currentUserId.value = sessionUserId
            refreshUserData(sessionUserId)
            refreshRecentSessions(sessionUserId)
        }
    }

    private fun refreshUserData(userId: String) {
        viewModelScope.launch {
            repository.fetchAndSyncUserProfile(userId, null)
            refreshRecentSessions(userId)
        }
    }

    fun refreshRecentSessions(userId: String = currentUserId.value ?: "") {
        if (userId.isBlank()) return
        viewModelScope.launch {
            try {
                val sessions = singlePlayerRepo.getRecentGameSessions(userId)
                _recentSessions.value = sessions
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            val user = repository.supabaseSignIn(email, pass)
            if (user != null) {
                currentUserId.value = user.id
                refreshUserData(user.id)
                _toastEvent.emit(ToastEvent.Show("Welcome back, ${user.displayName}!"))
            } else {
                _toastEvent.emit(ToastEvent.Show("Authentication failed. Check your connection or credentials.", true))
            }
        }
    }

    fun register(email: String, pass: String, name: String, refCode: String?) {
        viewModelScope.launch {
            val user = repository.supabaseSignUp(email, pass, name, refCode)
            if (user != null) {
                currentUserId.value = user.id
                refreshUserData(user.id)
                _toastEvent.emit(ToastEvent.Show("Account created! Welcome, ${user.displayName}."))
            } else {
                _toastEvent.emit(ToastEvent.Show("Registration failed. Please try again.", true))
            }
        }
    }

    fun logout() {
        com.example.data.local.SessionManager.clearSession()
        currentUserId.value = null
        _gameState.value = GameState()
    }

    fun updateProfile(name: String, photoUrl: String?) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            repository.updateUserProfile(userId, name, photoUrl)
            _toastEvent.emit(ToastEvent.Show("Profile updated!"))
        }
    }

    init {
        viewModelScope.launch {
            SupabaseClient.sessionExpired.collect { expired ->
                if (expired) {
                    timerJob?.cancel()
                    _gameState.value = GameState()
                    _toastEvent.emit(ToastEvent.Show("Session expired, please login again.", true))
                }
            }
        }
    }

    // ─── SINGLE-PLAYER PROGRESSIVE GAME ACTIONS ───────────────────────────────

    fun startGame() {
        val user = currentUser.value
        if (user == null) {
            viewModelScope.launch {
                _toastEvent.emit(ToastEvent.Show("Please login first to play.", true))
            }
            return
        }

        if (user.walletBalance < 10.0) {
            viewModelScope.launch {
                _toastEvent.emit(ToastEvent.Show("Minimum ₹10 balance required to play. Please Add Money.", true))
            }
            return
        }

        _gameState.value = GameState(isLoading = true, active = true)

        viewModelScope.launch {
            when (val res = singlePlayerRepo.startGame()) {
                is RpcResult.Ok -> {
                    val startResp = res.value
                    val sessionId = startResp.sessionId ?: ""
                    val entryFee = startResp.entryFee

                    if (startResp.newBalance != null) {
                        repository.updateLocalBalance(user.id, startResp.newBalance)
                    }

                    _gameState.value = GameState(
                        active = true,
                        sessionId = sessionId,
                        entryFee = entryFee,
                        questionNumber = 1,
                        score = 0,
                        correctAnswers = 0,
                        questionsAnswered = 0,
                        isLoading = true
                    )

                    loadQuestion(sessionId)
                }
                is RpcResult.Err -> {
                    _gameState.value = GameState()
                    _toastEvent.emit(ToastEvent.Show(res.message, true))
                }
            }
        }
    }

    fun retryLoadQuestion() {
        val state = _gameState.value
        val sid = state.sessionId ?: return
        _gameState.value = state.copy(isLoading = true, errorMessage = null)
        loadQuestion(sid)
    }

    private fun loadQuestion(sessionId: String) {
        viewModelScope.launch {
            val currentState = _gameState.value
            _gameState.value = currentState.copy(isLoading = true, errorMessage = null)

            when (val res = singlePlayerRepo.getNextQuestion(sessionId)) {
                is RpcResult.Ok -> {
                    val q = res.value
                    _gameState.value = _gameState.value.copy(
                        active = true,
                        isLoading = false,
                        errorMessage = null,
                        currentQuestion = q,
                        questionNumber = q.questionNumber,
                        score = currentState.score,
                        correctAnswers = currentState.correctAnswers,
                        timeLeftMs = q.timeLimitMs,
                        timeLimitMs = q.timeLimitMs,
                        isLocked = false,
                        selectedOption = null,
                        lastIsCorrect = null,
                        lastCorrectAnswer = null,
                        lastPointsEarned = 0,
                        isGameOver = false,
                        gameResult = null
                    )
                    startCountdownTimer()
                }
                is RpcResult.Err -> {
                    timerJob?.cancel()
                    if (res.retryable) {
                        _gameState.value = _gameState.value.copy(
                            isLoading = false,
                            errorMessage = res.message,
                            errorRetryable = true
                        )
                    } else {
                        _toastEvent.emit(ToastEvent.Show(res.message, true))
                        quitGame()
                    }
                }
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        questionStartTime = System.currentTimeMillis()

        timerJob = viewModelScope.launch {
            var remaining = _gameState.value.timeLeftMs
            while (remaining > 0 && _gameState.value.active && !_gameState.value.isLocked) {
                delay(50)
                remaining -= 50
                _gameState.value = _gameState.value.copy(timeLeftMs = maxOf(0L, remaining))
            }

            if (remaining <= 0 && !_gameState.value.isLocked && _gameState.value.active) {
                // Timeout auto-submit
                submitAnswer(null)
            }
        }
    }

    fun retrySubmitAnswer() {
        val state = _gameState.value
        val q = state.currentQuestion ?: return
        val sid = state.sessionId ?: return
        submitAnswerInternal(sid, q, state.selectedOption)
    }

    fun submitAnswer(answer: String?) {
        val state = _gameState.value
        if (state.isLocked || !state.active || state.isGameOver) return

        timerJob?.cancel()
        val question = state.currentQuestion ?: return
        val sessionId = state.sessionId ?: return

        _gameState.value = state.copy(
            isLocked = true,
            selectedOption = answer
        )

        submitAnswerInternal(sessionId, question, answer)
    }

    private fun submitAnswerInternal(sessionId: String, question: QuestionData, answer: String?) {
        val state = _gameState.value
        val responseTimeMs = (state.timeLimitMs - state.timeLeftMs).coerceAtLeast(0L)

        viewModelScope.launch {
            _gameState.value = _gameState.value.copy(isLoading = true, errorMessage = null)
            when (val res = singlePlayerRepo.submitAnswer(sessionId, question.questionId, answer, responseTimeMs)) {
                is RpcResult.Ok -> {
                    val submitResp = res.value
                    val isCorrect = submitResp.correct
                    val gameOver = submitResp.gameOver

                    val newScore = submitResp.totalScore
                    val newCorrect = if (isCorrect) state.correctAnswers + 1 else state.correctAnswers
                    val newAnswered = state.questionsAnswered + 1

                    if (!gameOver) {
                        _gameState.value = _gameState.value.copy(
                            isLoading = false,
                            isLocked = true,
                            selectedOption = answer,
                            lastIsCorrect = isCorrect,
                            lastCorrectAnswer = submitResp.correctAnswer ?: question.correctAnswer,
                            lastPointsEarned = submitResp.pointsEarned,
                            score = newScore,
                            correctAnswers = newCorrect,
                            questionsAnswered = newAnswered
                        )

                        delay(600)
                        loadQuestion(sessionId)
                    } else {
                        _gameState.value = _gameState.value.copy(
                            isLoading = false,
                            isLocked = true,
                            selectedOption = answer,
                            lastIsCorrect = false,
                            lastCorrectAnswer = submitResp.correctAnswer ?: question.correctAnswer,
                            lastPointsEarned = 0,
                            questionsAnswered = newAnswered
                        )

                        delay(800)

                        val result = GameResult(
                            totalScore = submitResp.totalScore,
                            correctAnswers = newCorrect,
                            questionsAnswered = newAnswered,
                            prize = submitResp.prize,
                            reason = submitResp.reason ?: if (answer == null) "Time Out" else "Wrong Answer"
                        )

                        _gameState.value = _gameState.value.copy(
                            active = false,
                            isGameOver = true,
                            gameResult = result,
                            score = submitResp.totalScore
                        )

                        currentUserId.value?.let { uid ->
                            refreshUserData(uid)
                        }
                    }
                }
                is RpcResult.Err -> {
                    _gameState.value = _gameState.value.copy(
                        isLoading = false,
                        errorMessage = res.message,
                        errorRetryable = true
                    )
                }
            }
        }
    }

    fun quitGame() {
        timerJob?.cancel()
        val sid = _gameState.value.sessionId
        if (sid != null) {
            viewModelScope.launch {
                singlePlayerRepo.endGame(sid)
                currentUserId.value?.let { refreshUserData(it) }
            }
        }
        _gameState.value = GameState()
    }

    fun refreshUserWallet() {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            repository.fetchAndSyncUserProfile(userId, null)
            refreshRecentSessions(userId)
        }
    }

    val userReferrals = currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else flow {
            emit(repository.getUserReferrals(id))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWithdrawals = currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getUserWithdrawalsFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startWithdrawalWatcher() {
        watcherJob?.cancel()
        val userId = currentUserId.value ?: return
        watcherJob = viewModelScope.launch {
            while (isActive) {
                val changed = repository.syncUserWithdrawals(userId)
                if (changed) {
                    refreshUserData(userId)
                }
                val pendingCount = database.speedMathDao().getPendingWithdrawalsCount(userId)
                if (pendingCount == 0) {
                    break
                }
                delay(5000L)
            }
        }
    }

    fun syncWithdrawals() {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            repository.syncUserWithdrawals(userId)
            refreshUserData(userId)
            _toastEvent.emit(ToastEvent.Show("Withdrawal status refreshed."))
            startWithdrawalWatcher()
        }
    }

    fun withdraw(
        amount: Double,
        method: String,
        account: String,
        accountHolder: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            withdrawSubmitting.value = true
            val result = repository.withdrawMoney(userId, amount, method, account, accountHolder)
            withdrawSubmitting.value = false

            _toastEvent.emit(ToastEvent.Show(result.message, isError = !result.success))
            onResult(result.success, result.message)

            if (result.success) {
                syncWithdrawals()
                startWithdrawalWatcher()
            }
        }
    }

    fun purchaseVip(tier: String, price: Double, days: Long) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            val success = repository.purchaseVipPass(userId, tier, price, days)
            if (success) {
                _toastEvent.emit(ToastEvent.Show("VIP Pass activated!"))
                refreshUserData(userId)
            } else {
                _toastEvent.emit(ToastEvent.Show("Purchase failed. Insufficient balance.", true))
            }
        }
    }

    fun selectChatRoom(roomSlug: String) {
        selectedChatRoom.value = roomSlug
        viewModelScope.launch {
            repository.syncChatRoomMessages(roomSlug)
        }
    }

    fun sendChat(text: String) {
        val user = currentUser.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendChatMessage(selectedChatRoom.value, user.id, user.displayName, text.trim())
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }
}
