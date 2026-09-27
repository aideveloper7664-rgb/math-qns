package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.SpeedMathRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class GameState(
    val active: Boolean = false,
    val isMatchmaking: Boolean = false,
    val mode: String = "1v1",
    val entryFee: Double = 0.0,
    val matchId: String? = null,
    val questions: List<QuestionEntity> = emptyList(),
    val currentIndex: Int = 0,
    val score: Int = 0,
    val correctCount: Int = 0,
    val answeredCount: Int = 0,
    val times: List<Double> = emptyList(),
    val totalTimePerQuestion: Int = 15,
    val remainingSeconds: Float = 15f,
    val isLocked: Boolean = false,
    val selectedOption: String? = null,
    val lastIsCorrect: Boolean? = null,
    val lastGainedPoints: Int = 0,
    val isFinished: Boolean = false,
    val isPractice: Boolean = false
)

sealed class ToastEvent {
    data class Show(val message: String, val isError: Boolean = false) : ToastEvent()
}

class SpeedMathViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SpeedMathRepository(AppDatabase.getDatabase(application).speedMathDao())
    private var timerJob: Job? = null
    private var questionStartTime: Long = 0

    private val _paymentUrlEvent = MutableSharedFlow<String>()
    val paymentUrlEvent: SharedFlow<String> = _paymentUrlEvent.asSharedFlow()

    val currentUserId = MutableStateFlow<String?>(null)

    val currentUser: StateFlow<UserEntity?> = currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getUserFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userMatchHistory: StateFlow<List<MatchParticipantEntity>> = currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getUserMatchParticipants(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        viewModelScope.launch {
            // Restore session persisted from a previous app launch
            val persistedUserId = com.example.data.remote.SupabaseClient.currentUserId
            val persistedToken = com.example.data.remote.SupabaseClient.authToken
            if (persistedUserId != null && persistedToken != null) {
                currentUserId.value = persistedUserId
                android.util.Log.d("AUTH", "✅ Session restored for userId=$persistedUserId")
            }
            repository.syncAllRealDataFromSupabase(currentUserId.value)
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            val user = repository.supabaseSignIn(email, pass)
            if (user != null) {
                currentUserId.value = user.id
                repository.syncAllRealDataFromSupabase(user.id)
                _toastEvent.emit(ToastEvent.Show("Welcome back, ${user.displayName}!"))
            } else {
                _toastEvent.emit(ToastEvent.Show("Sign in failed. Please check your credentials.", true))
            }
        }
    }

    fun register(email: String, pass: String, username: String, refCode: String?) {
        viewModelScope.launch {
            val user = repository.supabaseSignUp(email, pass, username, refCode)
            if (user != null) {
                currentUserId.value = user.id
                repository.syncAllRealDataFromSupabase(user.id)
                _toastEvent.emit(ToastEvent.Show("Account created! Welcome to SpeedMath Arena."))
            } else {
                _toastEvent.emit(ToastEvent.Show("Registration failed. Please try again.", true))
            }
        }
    }

    fun logout() {
        com.example.data.remote.SupabaseClient.clearSession()
        currentUserId.value = null
        viewModelScope.launch {
            _toastEvent.emit(ToastEvent.Show("Signed out successfully."))
        }
    }

    fun updateProfile(displayName: String, photoUrl: String?) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(
                displayName = displayName.ifBlank { user.displayName },
                photoUrl = photoUrl?.ifBlank { null }
            )
            repository.updateUser(updated)
            _toastEvent.emit(ToastEvent.Show("Profile updated."))
        }
    }

    fun startMatchmaking(mode: String, entryFee: Double) {
        val user = currentUser.value ?: return
        if (entryFee > 0 && user.walletBalance < entryFee) {
            viewModelScope.launch {
                _toastEvent.emit(ToastEvent.Show("Insufficient wallet balance.", true))
            }
            return
        }

        _gameState.value = GameState(
            active = false,
            isMatchmaking = true,
            mode = mode,
            entryFee = entryFee,
            isPractice = false
        )

        viewModelScope.launch {
            delay(2000)
            if (_gameState.value.isMatchmaking) {
                startMatchGame(mode, entryFee, false)
            }
        }
    }

    fun cancelMatchmaking() {
        _gameState.value = GameState()
    }

    fun startPracticeMode() {
        startMatchGame("practice", 0.0, true)
    }

    private fun startMatchGame(mode: String, entryFee: Double, isPractice: Boolean) {
        viewModelScope.launch {
            val questions = repository.getQuestionsForGame(10)
            _gameState.value = GameState(
                active = true,
                isMatchmaking = false,
                mode = mode,
                entryFee = entryFee,
                matchId = UUID.randomUUID().toString(),
                questions = questions,
                currentIndex = 0,
                score = 0,
                correctCount = 0,
                answeredCount = 0,
                times = emptyList(),
                totalTimePerQuestion = 15,
                remainingSeconds = 15f,
                isLocked = false,
                isFinished = false,
                isPractice = isPractice
            )
            startQuestionTimer()
        }
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        questionStartTime = System.currentTimeMillis()

        timerJob = viewModelScope.launch {
            val totalSec = _gameState.value.totalTimePerQuestion
            var currentSec = totalSec.toFloat()

            while (currentSec > 0f && _gameState.value.active && !_gameState.value.isLocked) {
                delay(100)
                currentSec -= 0.1f
                _gameState.value = _gameState.value.copy(remainingSeconds = maxOf(0f, currentSec))
            }

            if (currentSec <= 0f && !_gameState.value.isLocked) {
                submitAnswer(null)
            }
        }
    }

    fun submitAnswer(selectedOption: String?) {
        val state = _gameState.value
        if (state.isLocked || !state.active || state.isFinished) return

        timerJob?.cancel()
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        val elapsedSec = (System.currentTimeMillis() - questionStartTime) / 1000.0
        val isCorrect = selectedOption != null && selectedOption == currentQ.correctAnswer

        var gained = 0
        if (isCorrect) {
            val speedFactor = 0.5f + 0.5f * (state.remainingSeconds / state.totalTimePerQuestion)
            gained = (100 * speedFactor).toInt()
        }

        val newScore = state.score + gained
        val newCorrect = if (isCorrect) state.correctCount + 1 else state.correctCount
        val newAnswered = state.answeredCount + 1
        val newTimes = state.times + elapsedSec

        _gameState.value = state.copy(
            isLocked = true,
            selectedOption = selectedOption,
            lastIsCorrect = isCorrect,
            lastGainedPoints = gained,
            score = newScore,
            correctCount = newCorrect,
            answeredCount = newAnswered,
            times = newTimes
        )

        viewModelScope.launch {
            delay(1200)
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.questions.size) {
                finishGame()
            } else {
                _gameState.value = _gameState.value.copy(
                    currentIndex = nextIndex,
                    remainingSeconds = state.totalTimePerQuestion.toFloat(),
                    isLocked = false,
                    selectedOption = null,
                    lastIsCorrect = null,
                    lastGainedPoints = 0
                )
                startQuestionTimer()
            }
        }
    }

    private fun finishGame() {
        timerJob?.cancel()
        val state = _gameState.value
        val userId = currentUserId.value ?: return

        _gameState.value = state.copy(
            active = false,
            isFinished = true
        )

        viewModelScope.launch {
            repository.recordMatchResults(
                userId = userId,
                mode = state.mode,
                entryFee = state.entryFee,
                score = state.score,
                correctAnswers = state.correctCount,
                totalQuestions = state.questions.size,
                isPractice = state.isPractice
            )
        }
    }

    fun quitGame() {
        timerJob?.cancel()
        _gameState.value = GameState()
    }

    fun refreshUserWallet() {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            repository.fetchAndSyncUserProfile(userId, null)
        }
    }

    fun withdraw(amount: Double, method: String, account: String) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            val success = repository.withdrawMoney(userId, amount, method, account)
            if (success) {
                _toastEvent.emit(ToastEvent.Show("Withdrawal of ₹${amount.toInt()} requested."))
            } else {
                _toastEvent.emit(ToastEvent.Show("Withdrawal failed.", true))
            }
        }
    }

    fun purchaseVip(tier: String, price: Double, days: Long) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            val success = repository.purchaseVipPass(userId, tier, price, days)
            if (success) {
                _toastEvent.emit(ToastEvent.Show("VIP Pass activated!"))
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
