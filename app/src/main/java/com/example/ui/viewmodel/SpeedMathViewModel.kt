package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.*
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
    val errorRetryable: Boolean = true,
    val refundCredited: Boolean = false,
    val refundAmount: Double = 0.0
)

data class AppUpdateConfig(
    val latestVersion: String,
    val downloadUrl: String,
    val forceUpdate: Boolean = false
)

data class HomeButtonsUi(
    val startGameText: String = "⚡ PLAY SPEED MATH",
    val startGameSubtext: String = "Compete live & win real cash",
    val practiceText: String = "🎯 PRACTICE MODE",
    val practiceSubtext: String = "Free unlimited practice"
)

data class GameConfigUi(
    val entryFee: Double = 10.0,
    val showPlayConfirm: Boolean = true,
    val playTitle: String = "Start Game?",
    val playMessage: String = "{entry_fee} will be deducted from your wallet to start the game.",
    val showResultPopup: Boolean = true,
    val winTitle: String = "🎉 You Won!",
    val winMessage: String = "",
    val loseTitle: String = "Game Over",
    val loseMessage: String = "Better luck next time!",
    val questionsPerGame: Int = 12
)

data class HistoryUiState(
    val items: List<GameHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class LeaderboardUiState(
    val metric: String = "best_score",
    val rows: List<LeaderboardRow> = emptyList(),
    val me: LeaderboardMe? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val updatedAtMs: Long? = null
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

    private val _leaderboardState = MutableStateFlow(LeaderboardUiState())
    val leaderboardState: StateFlow<LeaderboardUiState> = _leaderboardState.asStateFlow()
    private var leaderboardAutoJob: Job? = null
    private var leaderboardLoadJob: Job? = null

    private val _gameConfig = MutableStateFlow(GameConfigUi())
    val gameConfig: StateFlow<GameConfigUi> = _gameConfig.asStateFlow()

    val appUpdateConfig = MutableStateFlow<AppUpdateConfig?>(null)
    val homeButtonsConfig = MutableStateFlow(HomeButtonsUi())
    val activeAnnouncements = MutableStateFlow<List<SupabaseAnnouncementDto>>(emptyList())

    private val _showPlayConfirm = MutableStateFlow(false)
    val showPlayConfirm: StateFlow<Boolean> = _showPlayConfirm.asStateFlow()

    private val _gameHistory = MutableStateFlow(HistoryUiState())
    val gameHistory: StateFlow<HistoryUiState> = _gameHistory.asStateFlow()

    val photoUploading = MutableStateFlow(false)

    private val _vipPlans = MutableStateFlow<List<SupabaseVipPlanDto>>(emptyList())
    val vipPlans: StateFlow<List<SupabaseVipPlanDto>> = _vipPlans.asStateFlow()
    val vipLoading = MutableStateFlow(false)

    private val _liveTournaments = MutableStateFlow<List<TournamentLiveDto>>(emptyList())
    val liveTournaments: StateFlow<List<TournamentLiveDto>> = _liveTournaments.asStateFlow()
    val tournamentsLoading = MutableStateFlow(false)
    val joinedTournamentIds = MutableStateFlow<Set<String>>(emptySet())
    val tournamentJoiningId = MutableStateFlow<String?>(null)
    private var tournamentsAutoJob: Job? = null

    private val _knockoutTournaments = MutableStateFlow<List<KnockoutTournamentDto>>(emptyList())
    val knockoutTournaments: StateFlow<List<KnockoutTournamentDto>> = _knockoutTournaments.asStateFlow()
    val knockoutError = MutableStateFlow<String?>(null)
    val knockoutMatchups = MutableStateFlow<List<KnockoutMatchupDto>>(emptyList())
    val knockoutParticipants = MutableStateFlow<List<KnockoutParticipantDto>>(emptyList())
    val knockoutLoading = MutableStateFlow(false)
    val joinedKnockoutIds = MutableStateFlow<Set<String>>(emptySet())
    val knockoutJoiningId = MutableStateFlow<String?>(null)

    private var refundToastShownForSession = false

    fun setLeaderboardMetric(metric: String) {
        if (metric == _leaderboardState.value.metric) return
        _leaderboardState.value = _leaderboardState.value.copy(
            metric = metric, rows = emptyList(), me = null, errorMessage = null, updatedAtMs = null
        )
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        val metric = _leaderboardState.value.metric
        leaderboardLoadJob?.cancel()
        leaderboardLoadJob = viewModelScope.launch {
            _leaderboardState.value = _leaderboardState.value.copy(isLoading = true)
            when (val r = repository.fetchLeaderboard(metric)) {
                is RpcResult.Ok -> {
                    if (_leaderboardState.value.metric == metric) {
                        _leaderboardState.value = _leaderboardState.value.copy(
                            rows = r.value.rows, me = r.value.me, isLoading = false,
                            errorMessage = null, updatedAtMs = System.currentTimeMillis()
                        )
                    }
                }
                is RpcResult.Err -> {
                    if (_leaderboardState.value.metric == metric) {
                        _leaderboardState.value = _leaderboardState.value.copy(
                            isLoading = false, errorMessage = r.message
                        ) // rows are kept on purpose
                    }
                }
            }
        }
    }

    fun startLeaderboardAutoRefresh() {
        leaderboardAutoJob?.cancel()
        leaderboardAutoJob = viewModelScope.launch {
            while (isActive) {
                loadLeaderboard()
                delay(30_000)
            }
        }
    }

    fun stopLeaderboardAutoRefresh() {
        leaderboardAutoJob?.cancel()
        leaderboardAutoJob = null
    }

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
            loadUserJoinedTournaments()
        }
        loadGameConfig()
        loadVipPlans()
        loadLiveTournaments()
        loadKnockoutTournaments()
        loadAnnouncements()
    }

    fun loadUserJoinedTournaments() {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                val res = SupabaseClient.restApi.getUserTournamentParticipants(userQuery = "eq.$uid")
                if (res.isSuccessful && res.body() != null) {
                    val ids = res.body()!!.mapNotNull { it["tournament_id"] }
                    if (ids.isNotEmpty()) {
                        joinedTournamentIds.value = joinedTournamentIds.value + ids.toSet()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ViewModel", "Failed to load joined tournaments: ${e.message}")
            }
        }
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            try {
                val res = SupabaseClient.restApi.getAnnouncements()
                if (res.isSuccessful && res.body() != null) {
                    activeAnnouncements.value = res.body()!!.filter { (it.status ?: "active") == "active" }
                }
            } catch (e: Exception) {
                android.util.Log.e("ViewModel", "Failed to load announcements: ${e.message}")
            }
        }
    }

    private fun isVersionHigher(v1: String, v2: String): Boolean {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 > p2) return true
            if (p1 < p2) return false
        }
        return false
    }

    fun loadGameConfig() {
        viewModelScope.launch {
            when (val r = repository.fetchGameConfig()) {
                is RpcResult.Ok -> {
                    var current = _gameConfig.value
                    for (row in r.value) {
                        val map = row.value ?: continue
                        when (row.key) {
                            "popup_settings" -> {
                                val showConfirm = (map["show_play_confirmation"] as? Boolean) ?: current.showPlayConfirm
                                val playT = (map["play_confirm_title"] as? String) ?: current.playTitle
                                val playM = (map["play_confirm_message"] as? String) ?: current.playMessage
                                val showRes = (map["show_result_popup"] as? Boolean) ?: current.showResultPopup
                                val winT = (map["result_title_win"] as? String) ?: current.winTitle
                                val winM = (map["result_message_win"] as? String) ?: current.winMessage
                                val loseT = (map["result_title_lose"] as? String) ?: current.loseTitle
                                val loseM = (map["result_message_lose"] as? String) ?: current.loseMessage

                                current = current.copy(
                                    showPlayConfirm = showConfirm,
                                    playTitle = playT,
                                    playMessage = playM,
                                    showResultPopup = showRes,
                                    winTitle = winT,
                                    winMessage = winM,
                                    loseTitle = loseT,
                                    loseMessage = loseM
                                )
                            }
                            "game_rewards" -> {
                                val fee = (map["entry_fee"] as? Number)?.toDouble() ?: current.entryFee
                                val qCount = (map["questions_per_game"] as? Number)?.toInt() ?: current.questionsPerGame
                                current = current.copy(
                                    entryFee = fee,
                                    questionsPerGame = qCount
                                )
                            }
                            "app_version" -> {
                                val latest = (map["latest_version"] as? String) ?: (map["version"] as? String)
                                val url = (map["download_url"] as? String) ?: (map["url"] as? String) ?: "https://ais-dev-cfcha536thi2azarfnw6uq-840513166105.asia-southeast1.run.app/app-debug.apk"
                                val force = (map["force_update"] as? Boolean) ?: false
                                if (!latest.isNullOrBlank()) {
                                    val currentVer = com.example.BuildConfig.VERSION_NAME
                                    if (isVersionHigher(latest, currentVer)) {
                                        appUpdateConfig.value = AppUpdateConfig(latest, url, force)
                                    }
                                }
                            }
                            "home_buttons" -> {
                                val curButtons = homeButtonsConfig.value
                                val stText = (map["start_game_text"] as? String) ?: curButtons.startGameText
                                val stSub = (map["start_game_subtext"] as? String) ?: curButtons.startGameSubtext
                                val prText = (map["practice_text"] as? String) ?: curButtons.practiceText
                                val prSub = (map["practice_subtext"] as? String) ?: curButtons.practiceSubtext
                                homeButtonsConfig.value = HomeButtonsUi(stText, stSub, prText, prSub)
                            }
                        }
                    }
                    _gameConfig.value = current
                }
                is RpcResult.Err -> {
                    // Keep defaults
                }
            }
        }
    }

    private fun refreshUserData(userId: String) {
        viewModelScope.launch {
            repository.fetchAndSyncUserProfile(userId, null)
            refreshRecentSessions(userId)
            loadUserJoinedTournaments()
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
                loadGameConfig()
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
                loadGameConfig()
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

    fun uploadProfilePhoto(base64: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            photoUploading.value = true
            when (val r = repository.uploadProfilePhoto(base64)) {
                is RpcResult.Ok -> {
                    photoUploading.value = false
                    _toastEvent.emit(ToastEvent.Show("Photo updated"))
                    refreshUserData(uid)
                    loadLeaderboard()
                }
                is RpcResult.Err -> {
                    photoUploading.value = false
                    _toastEvent.emit(ToastEvent.Show(r.message, true))
                }
            }
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

    fun requestPlay() {
        val user = currentUser.value
        if (user == null) {
            viewModelScope.launch {
                _toastEvent.emit(ToastEvent.Show("Please login first to play.", true))
            }
            return
        }

        val fee = _gameConfig.value.entryFee
        if (user.walletBalance < fee) {
            val feeFormatted = if (fee % 1.0 == 0.0) "₹${fee.toInt()}" else "₹${"%.2f".format(fee)}"
            viewModelScope.launch {
                _toastEvent.emit(ToastEvent.Show("Minimum $feeFormatted balance required to play. Please Add Money.", true))
            }
            return
        }

        loadGameConfig()

        if (_gameConfig.value.showPlayConfirm) {
            _showPlayConfirm.value = true
        } else {
            startGame()
        }
    }

    fun dismissPlayConfirm() {
        _showPlayConfirm.value = false
    }

    fun confirmPlay() {
        _showPlayConfirm.value = false
        startGame()
    }

    fun startGame() {
        val user = currentUser.value
        if (user == null) {
            viewModelScope.launch {
                _toastEvent.emit(ToastEvent.Show("Please login first to play.", true))
            }
            return
        }

        val requiredFee = _gameConfig.value.entryFee
        if (user.walletBalance < requiredFee) {
            val feeFormatted = if (requiredFee % 1.0 == 0.0) "₹${requiredFee.toInt()}" else "₹${"%.2f".format(requiredFee)}"
            viewModelScope.launch {
                _toastEvent.emit(ToastEvent.Show("Minimum $feeFormatted balance required to play. Please Add Money.", true))
            }
            return
        }

        refundToastShownForSession = false
        _gameState.value = GameState(isLoading = true, active = true, entryFee = requiredFee)

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
            when (val res = singlePlayerRepo.submitAnswer(
                sessionId = sessionId,
                questionId = question.questionId,
                answer = answer,
                responseTimeMs = responseTimeMs
            )) {
                is RpcResult.Ok -> {
                    val submitResp = res.value
                    val isCorrect = submitResp.correct
                    val gameOver = submitResp.gameOver

                    val newScore = submitResp.totalScore
                    val newCorrect = if (isCorrect) state.correctAnswers + 1 else state.correctAnswers
                    val newAnswered = state.questionsAnswered + 1

                    // F7: Entry fee refund display & immediate refresh
                    if (submitResp.refundApplied && submitResp.refund > 0 && !refundToastShownForSession) {
                        refundToastShownForSession = true
                        _toastEvent.emit(ToastEvent.Show("🎉 Entry Fee Refunded: ₹${"%.2f".format(submitResp.refund)}"))
                        currentUserId.value?.let { refreshUserData(it) }
                    }

                    if (!gameOver) {
                        _gameState.value = _gameState.value.copy(
                            isLoading = false,
                            isLocked = true,
                            selectedOption = answer,
                            lastIsCorrect = isCorrect,
                            lastCorrectAnswer = submitResp.correctAnswer,
                            lastPointsEarned = submitResp.pointsEarned,
                            score = newScore,
                            correctAnswers = newCorrect,
                            questionsAnswered = newAnswered,
                            refundCredited = submitResp.refundApplied,
                            refundAmount = submitResp.refund
                        )

                        delay(600)
                        loadQuestion(sessionId)
                    } else {
                        _gameState.value = _gameState.value.copy(
                            isLoading = false,
                            isLocked = true,
                            selectedOption = answer,
                            lastIsCorrect = false,
                            lastCorrectAnswer = submitResp.correctAnswer,
                            lastPointsEarned = 0,
                            questionsAnswered = newAnswered,
                            refundCredited = submitResp.refundApplied,
                            refundAmount = submitResp.refund
                        )

                        delay(800)

                        val result = GameResult(
                            totalScore = submitResp.totalScore,
                            correctAnswers = newCorrect,
                            questionsAnswered = newAnswered,
                            prize = submitResp.prize,
                            reason = submitResp.reason ?: if (answer == null) "Time Out" else "Wrong Answer",
                            refund = submitResp.refund,
                            refundApplied = submitResp.refundApplied
                        )

                        currentUserId.value?.let { uid ->
                            refreshUserData(uid)
                            loadLeaderboard()
                        }

                        // F2: If showResultPopup is false, skip popup, show toast and return home
                        if (!_gameConfig.value.showResultPopup) {
                            _toastEvent.emit(ToastEvent.Show("Game over. Score ${submitResp.totalScore}"))
                            quitGame()
                        } else {
                            _gameState.value = _gameState.value.copy(
                                active = false,
                                isGameOver = true,
                                gameResult = result,
                                score = submitResp.totalScore
                            )
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

    fun loadGameHistory() {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            _gameHistory.value = _gameHistory.value.copy(isLoading = true, error = null)
            when (val r = repository.fetchGameHistory(uid)) {
                is RpcResult.Ok -> {
                    _gameHistory.value = HistoryUiState(items = r.value, isLoading = false, error = null)
                }
                is RpcResult.Err -> {
                    _gameHistory.value = _gameHistory.value.copy(isLoading = false, error = r.message)
                }
            }
        }
    }

    fun loadVipPlans() {
        viewModelScope.launch {
            vipLoading.value = true
            when (val r = repository.fetchVipPlans()) {
                is RpcResult.Ok -> {
                    _vipPlans.value = r.value.filter { it.enabled != false }
                    vipLoading.value = false
                }
                is RpcResult.Err -> {
                    vipLoading.value = false
                }
            }
        }
    }

    fun purchaseVip(tier: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            vipLoading.value = true
            when (val r = repository.purchaseVip(tier)) {
                is RpcResult.Ok -> {
                    vipLoading.value = false
                    refreshUserData(uid)
                    val dateStr = r.value.expiresAt ?: ""
                    _toastEvent.emit(ToastEvent.Show("VIP activated${if (dateStr.isNotBlank()) " until $dateStr" else "!"}"))
                }
                is RpcResult.Err -> {
                    vipLoading.value = false
                    _toastEvent.emit(ToastEvent.Show(r.message, true))
                }
            }
        }
    }

    fun loadLiveTournaments() {
        viewModelScope.launch {
            tournamentsLoading.value = true
            when (val r = repository.fetchLiveTournaments()) {
                is RpcResult.Ok -> {
                    _liveTournaments.value = r.value
                    tournamentsLoading.value = false
                }
                is RpcResult.Err -> {
                    tournamentsLoading.value = false
                }
            }
        }
    }

    fun joinLiveTournament(tournamentId: String) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            tournamentJoiningId.value = tournamentId
            val result = repository.joinLiveTournament(tournamentId)
            tournamentJoiningId.value = null
            when (result) {
                is RpcResult.Ok -> {
                    val resp = result.value
                    joinedTournamentIds.value = joinedTournamentIds.value + tournamentId
                    val feeMsg = if (resp.entryFeePaid != null && resp.entryFeePaid > 0) " ₹${resp.entryFeePaid.toInt()} deducted." else ""
                    val balMsg = if (resp.newBalance != null) " Balance: ₹${"%.2f".format(resp.newBalance)}" else ""
                    _toastEvent.emit(ToastEvent.Show("✅ Successfully joined tournament!$feeMsg$balMsg"))
                    refreshUserData(userId)
                    loadLiveTournaments()
                }
                is RpcResult.Err -> {
                    _toastEvent.emit(ToastEvent.Show(result.message, isError = true))
                }
            }
        }
    }

    fun startTournamentsAutoRefresh() {
        tournamentsAutoJob?.cancel()
        tournamentsAutoJob = viewModelScope.launch {
            while (isActive) {
                loadLiveTournaments()
                delay(60_000L)
            }
        }
    }

    fun stopTournamentsAutoRefresh() {
        tournamentsAutoJob?.cancel()
        tournamentsAutoJob = null
    }

    fun loadKnockoutTournaments() {
        viewModelScope.launch {
            knockoutLoading.value = true
            knockoutError.value = null
            when (val r = repository.fetchKnockoutTournaments()) {
                is RpcResult.Ok -> {
                    _knockoutTournaments.value = r.value
                    knockoutLoading.value = false
                }
                is RpcResult.Err -> {
                    knockoutError.value = r.message
                    knockoutLoading.value = false
                }
            }
            loadUserJoinedKnockouts()
        }
    }

    fun loadUserJoinedKnockouts() {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            val joined = repository.fetchUserJoinedKnockoutIds(userId)
            joinedKnockoutIds.value = joined
        }
    }

    fun joinKnockoutTournament(tournamentId: String) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            knockoutJoiningId.value = tournamentId
            val result = repository.joinKnockoutTournament(tournamentId)
            knockoutJoiningId.value = null
            when (result) {
                is RpcResult.Ok -> {
                    val resp = result.value
                    joinedKnockoutIds.value = joinedKnockoutIds.value + tournamentId
                    val feeMsg = if (resp.entryFeePaid != null && resp.entryFeePaid > 0) " ₹${resp.entryFeePaid.toInt()} deducted." else ""
                    val balMsg = if (resp.newBalance != null) " Balance: ₹${"%.2f".format(resp.newBalance)}" else ""
                    _toastEvent.emit(ToastEvent.Show("✅ Successfully joined tournament!$feeMsg$balMsg"))
                    refreshUserData(userId)
                    loadKnockoutTournaments()
                }
                is RpcResult.Err -> {
                    _toastEvent.emit(ToastEvent.Show(result.message, isError = true))
                }
            }
        }
    }

    fun loadKnockoutBracket(tournamentId: String) {
        viewModelScope.launch {
            knockoutLoading.value = true
            knockoutMatchups.value = emptyList()
            knockoutParticipants.value = emptyList()

            val mRes = repository.fetchKnockoutMatchups(tournamentId)
            val pRes = repository.fetchKnockoutParticipants(tournamentId)

            if (mRes is RpcResult.Ok) {
                knockoutMatchups.value = mRes.value
            }
            if (pRes is RpcResult.Ok) {
                knockoutParticipants.value = pRes.value
            }
            knockoutLoading.value = false
        }
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
        if (watcherJob?.isActive == true) return
        val userId = currentUserId.value ?: return
        watcherJob = viewModelScope.launch {
            try {
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected when job is cancelled
                throw e
            } catch (e: Exception) {
                android.util.Log.e("WithdrawWatcher", "Watcher error: ${e.message}")
            }
        }
    }

    fun syncWithdrawals() {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                repository.syncUserWithdrawals(userId)
                refreshUserData(userId)
                _toastEvent.emit(ToastEvent.Show("Withdrawal status refreshed."))
                startWithdrawalWatcher()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("WithdrawSync", "Sync error: ${e.message}")
            }
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
