package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ArenaBottomNav
import com.example.ui.components.ArenaTopBar
import com.example.ui.components.PlayConfirmationDialog
import com.example.ui.screens.*
import com.example.ui.wallet.*
import com.example.ui.theme.SpeedMathTheme
import com.example.ui.viewmodel.SpeedMathViewModel
import com.example.ui.viewmodel.ToastEvent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // RESTORE SESSION before any data fetch
        com.example.data.remote.SupabaseClient.init(this)
        com.example.data.local.SessionManager.loadSession()
        val sessionUserId = com.example.data.local.SessionManager.userId
        val sessionEmail = com.example.data.local.SessionManager.userEmail
        if (sessionUserId != null) {
            android.util.Log.d("AUTH", "✅ Session restored for $sessionEmail ($sessionUserId)")
        } else {
            android.util.Log.d("AUTH", "⚠️ No session found — user not logged in")
        }

        setContent {
            SpeedMathTheme {
                val viewModel: SpeedMathViewModel = viewModel()
                val context = LocalContext.current

                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
                val userReferrals by viewModel.userReferrals.collectAsStateWithLifecycle()
                val userWithdrawals by viewModel.userWithdrawals.collectAsStateWithLifecycle()
                val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
                val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
                val allTournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
                val allKnockoutTournaments by viewModel.allKnockoutTournaments.collectAsStateWithLifecycle()
                val notifications by viewModel.notifications.collectAsStateWithLifecycle()
                val unreadNotifsCount by viewModel.unreadNotifsCount.collectAsStateWithLifecycle()
                val allBadges by viewModel.allBadges.collectAsStateWithLifecycle()
                val selectedChatRoom by viewModel.selectedChatRoom.collectAsStateWithLifecycle()
                val chatMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()
                val gameState by viewModel.gameState.collectAsStateWithLifecycle()
                val withdrawSubmitting by viewModel.withdrawSubmitting.collectAsStateWithLifecycle()

                val gameConfig by viewModel.gameConfig.collectAsStateWithLifecycle()
                val showPlayConfirm by viewModel.showPlayConfirm.collectAsStateWithLifecycle()
                val gameHistory by viewModel.gameHistory.collectAsStateWithLifecycle()
                val photoUploading by viewModel.photoUploading.collectAsStateWithLifecycle()
                val vipPlans by viewModel.vipPlans.collectAsStateWithLifecycle()
                val vipLoading by viewModel.vipLoading.collectAsStateWithLifecycle()
                val liveTournaments by viewModel.liveTournaments.collectAsStateWithLifecycle()
                val tournamentsLoading by viewModel.tournamentsLoading.collectAsStateWithLifecycle()
                val joinedTournamentIds by viewModel.joinedTournamentIds.collectAsStateWithLifecycle()
                val tournamentJoiningId by viewModel.tournamentJoiningId.collectAsStateWithLifecycle()
                val knockoutTournaments by viewModel.knockoutTournaments.collectAsStateWithLifecycle()
                val knockoutLoading by viewModel.knockoutLoading.collectAsStateWithLifecycle()
                val knockoutError by viewModel.knockoutError.collectAsStateWithLifecycle()
                val knockoutMatchups by viewModel.knockoutMatchups.collectAsStateWithLifecycle()
                val knockoutParticipants by viewModel.knockoutParticipants.collectAsStateWithLifecycle()
                val joinedKnockoutIds by viewModel.joinedKnockoutIds.collectAsStateWithLifecycle()
                val knockoutJoiningId by viewModel.knockoutJoiningId.collectAsStateWithLifecycle()

                val supportConfig by viewModel.supportConfig.collectAsStateWithLifecycle()

                val appUpdateConfig by viewModel.appUpdateConfig.collectAsStateWithLifecycle()
                val homeButtonsConfig by viewModel.homeButtonsConfig.collectAsStateWithLifecycle()
                val activeAnnouncements by viewModel.activeAnnouncements.collectAsStateWithLifecycle()

                var currentRoute by remember { mutableStateOf("home") }
                var activeCheckoutUrl by remember { mutableStateOf<String?>(null) }
                var activeDepositId by remember { mutableStateOf<String?>(null) }

                // App Lifecycle Observer for auto-syncing withdrawals on resume
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            viewModel.syncWithdrawals()
                            viewModel.startWithdrawalWatcher()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Toast event handler
                LaunchedEffect(Unit) {
                    viewModel.toastEvent.collect { event ->
                        when (event) {
                            is ToastEvent.Show -> {
                                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // Payment Redirect Handler (In-App WebView, NOT external browser)
                LaunchedEffect(Unit) {
                    viewModel.paymentUrlEvent.collect { url ->
                        activeCheckoutUrl = url
                        currentRoute = "checkout"
                    }
                }

                // Global Payment Activity Listener
                DisposableEffect(Unit) {
                    (application as? SpeedMathApp)?.paymentListener = { success, depositId, message ->
                        if (success) {
                            activeDepositId = depositId
                            currentRoute = "payment_processing"
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            currentRoute = "wallet"
                        }
                    }
                    onDispose {
                        (application as? SpeedMathApp)?.paymentListener = null
                    }
                }

                // Play confirmation popup dialog
                if (showPlayConfirm) {
                    PlayConfirmationDialog(
                        entryFee = gameConfig.entryFee,
                        title = gameConfig.playTitle,
                        messageTemplate = gameConfig.playMessage,
                        onConfirm = { viewModel.confirmPlay() },
                        onDismiss = { viewModel.dismissPlayConfirm() }
                    )
                }

                // Handle Back Button
                if (currentRoute != "home" && !gameState.active && !gameState.isGameOver) {
                    BackHandler {
                        when (currentRoute) {
                            "checkout" -> currentRoute = "deposit"
                            "payment_processing" -> currentRoute = "wallet"
                            "deposit", "add_money" -> currentRoute = "wallet"
                            "withdraw", "transactions" -> currentRoute = "wallet"
                            "settings" -> currentRoute = "profile"
                            "game_history", "history" -> currentRoute = "home"
                            "tournaments", "live_tournaments" -> currentRoute = "home"
                            else -> currentRoute = "home"
                        }
                    }
                }

                if (currentUser == null) {
                    AuthScreen(
                        onLogin = { email, pass ->
                            viewModel.login(email, pass)
                        },
                        onRegister = { email, pass, name, refCode ->
                            viewModel.register(email, pass, name, refCode)
                        }
                    )
                } else if (gameState.active || gameState.isGameOver) {
                    GameScreen(
                        state = gameState,
                        config = gameConfig,
                        user = currentUser,
                        onAnswer = { answer ->
                            viewModel.submitAnswer(answer)
                        },
                        onPlayAgain = {
                            viewModel.startGame()
                        },
                        onGoHome = {
                            viewModel.quitGame()
                            currentRoute = "home"
                        },
                        onViewHistory = {
                            viewModel.quitGame()
                            currentRoute = "game_history"
                        },
                        onRetry = {
                            if (gameState.currentQuestion == null) viewModel.retryLoadQuestion()
                            else viewModel.retrySubmitAnswer()
                        },
                        onQuit = {
                            viewModel.quitGame()
                            currentRoute = "home"
                        }
                    )
                } else {
                    Scaffold(
                        topBar = {
                            ArenaTopBar(
                                user = currentUser,
                                unreadNotifsCount = unreadNotifsCount,
                                onNotificationsClick = {
                                    viewModel.markNotificationsRead()
                                    currentRoute = "notifications"
                                },
                                onProfileClick = {
                                    currentRoute = "profile"
                                }
                            )
                        },
                        bottomBar = {
                            if (currentRoute in listOf("home", "wallet", "leaderboard", "chat", "profile")) {
                                ArenaBottomNav(
                                    currentRoute = currentRoute,
                                    onNavigate = { route -> currentRoute = route }
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = currentRoute,
                                label = "screen_transition"
                            ) { route ->
                                when (route) {
                                    "home" -> HomeScreen(
                                        user = currentUser,
                                        recentSessions = recentSessions,
                                        announcements = activeAnnouncements,
                                        homeButtonsConfig = homeButtonsConfig,
                                        onStartGame = {
                                            viewModel.requestPlay()
                                        },
                                        onNavigate = { target -> currentRoute = target }
                                    )

                                    "wallet" -> WalletScreen(
                                        user = currentUser,
                                        transactions = userTransactions,
                                        onNavigate = { target -> currentRoute = target },
                                        onRefreshWithdrawals = { viewModel.syncWithdrawals(silent = true) }
                                    )

                                    "deposit", "add_money" -> AddMoneyScreen(
                                        onNavigateToCheckout = { url, depId ->
                                            activeCheckoutUrl = url
                                            activeDepositId = depId
                                            currentRoute = "checkout"
                                        },
                                        onBack = { currentRoute = "wallet" }
                                    )

                                    "checkout" -> CheckoutScreen(
                                        checkoutUrl = activeCheckoutUrl ?: "",
                                        depositId = activeDepositId ?: "",
                                        onPaymentDetected = {
                                            currentRoute = "payment_processing"
                                        },
                                        onFailure = { errorMsg ->
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                            currentRoute = "wallet"
                                        },
                                        onCancel = {
                                            currentRoute = "wallet"
                                        }
                                    )

                                    "payment_processing" -> PaymentProcessingScreen(
                                        depositId = activeDepositId ?: "",
                                        onSuccess = { amount ->
                                            viewModel.refreshUserWallet()
                                            Toast.makeText(context, "₹${amount.toInt()} credited to your wallet!", Toast.LENGTH_LONG).show()
                                            currentRoute = "wallet"
                                        },
                                        onFailure = { errorMsg ->
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                            currentRoute = "wallet"
                                        }
                                    )

                                    "withdraw" -> WithdrawScreen(
                                        user = currentUser,
                                        withdrawals = userWithdrawals,
                                        submitting = withdrawSubmitting,
                                        onWithdraw = { amt, method, acc, holder, onResult ->
                                            viewModel.withdraw(amt, method, acc, holder) { success, _ ->
                                                onResult(success)
                                            }
                                        },
                                        onRefreshWithdrawals = { viewModel.syncWithdrawals(silent = false) },
                                        onBack = { currentRoute = "wallet" }
                                    )

                                    "transactions" -> TransactionsScreen(
                                        transactions = userTransactions,
                                        onBack = { currentRoute = "wallet" }
                                    )

                                    "leaderboard" -> LeaderboardScreen(
                                        currentUserId = currentUser?.id,
                                        state = leaderboardState,
                                        onMetricChange = { viewModel.setLeaderboardMetric(it) },
                                        onRefresh = { viewModel.loadLeaderboard() },
                                        onStartAutoRefresh = { viewModel.startLeaderboardAutoRefresh() },
                                        onStopAutoRefresh = { viewModel.stopLeaderboardAutoRefresh() }
                                    )

                                    "chat" -> ChatScreen(
                                        user = currentUser,
                                        messages = chatMessages,
                                        selectedRoom = selectedChatRoom,
                                        onSelectRoom = { room ->
                                            viewModel.selectChatRoom(room)
                                        },
                                        onSendMessage = { text ->
                                            viewModel.sendChat(text)
                                        }
                                    )

                                    "vip" -> VipScreen(
                                        user = currentUser,
                                        plans = vipPlans,
                                        isLoading = vipLoading,
                                        onPurchaseVip = { tier ->
                                            viewModel.purchaseVip(tier)
                                        },
                                        onBack = { currentRoute = "home" }
                                    )

                                    "referral" -> ReferralScreen(
                                        user = currentUser,
                                        referrals = userReferrals,
                                        onBack = { currentRoute = "home" }
                                    )

                                    "badges" -> BadgesScreen(
                                        badges = allBadges,
                                        onBack = { currentRoute = "home" }
                                    )

                                    "practice" -> PracticeScreen(
                                        onBack = { currentRoute = "home" }
                                    )

                                    "support" -> com.example.ui.screens.SupportScreen(
                                        config = supportConfig,
                                        onBack = { currentRoute = "profile" }
                                    )

                                    "policies" -> PoliciesScreen(
                                        onBack = { currentRoute = "home" }
                                    )

                                    "tournaments", "live_tournaments" -> TournamentsScreen(
                                        tournaments = liveTournaments,
                                        isLoading = tournamentsLoading,
                                        joinedTournamentIds = joinedTournamentIds,
                                        joiningTournamentId = tournamentJoiningId,
                                        onJoinTournament = { id -> viewModel.joinLiveTournament(id) },
                                        onRefresh = { viewModel.loadLiveTournaments() },
                                        onStartAutoRefresh = { viewModel.startTournamentsAutoRefresh() },
                                        onStopAutoRefresh = { viewModel.stopTournamentsAutoRefresh() },
                                        onBack = { currentRoute = "home" }
                                    )

                                    "game_history", "history" -> GameHistoryScreen(
                                        state = gameHistory,
                                        onRefresh = { viewModel.loadGameHistory() },
                                        onBack = { currentRoute = "home" }
                                    )

                                    "knockout" -> KnockoutScreen(
                                        tournaments = knockoutTournaments,
                                        isLoading = knockoutLoading,
                                        errorMessage = knockoutError,
                                        onRefresh = { viewModel.loadKnockoutTournaments() },
                                        onLoadBracket = { id -> viewModel.loadKnockoutBracket(id) },
                                        bracketMatchups = knockoutMatchups,
                                        bracketParticipants = knockoutParticipants,
                                        bracketLoading = knockoutLoading,
                                        joinedTournamentIds = joinedKnockoutIds,
                                        joiningTournamentId = knockoutJoiningId,
                                        onJoinTournament = { id -> viewModel.joinKnockoutTournament(id) },
                                        onBack = { currentRoute = "home" }
                                    )

                                    "profile" -> ProfileScreen(
                                        user = currentUser,
                                        withdrawals = userWithdrawals,
                                        photoUploading = photoUploading,
                                        onUpdateProfile = { name, photo ->
                                            viewModel.updateProfile(name, photo)
                                        },
                                        onUploadPhoto = { base64 ->
                                            viewModel.uploadProfilePhoto(base64)
                                        },
                                        onNavigate = { target -> currentRoute = target }
                                    )

                                    "settings" -> SettingsScreen(
                                        user = currentUser,
                                        onLogout = {
                                            viewModel.logout()
                                            currentRoute = "home"
                                        },
                                        onBack = { currentRoute = "profile" }
                                    )

                                    "notifications" -> NotificationsScreen(
                                        notifications = notifications,
                                        onBack = { currentRoute = "home" }
                                    )

                                    else -> HomeScreen(
                                        user = currentUser,
                                        recentSessions = recentSessions,
                                        announcements = activeAnnouncements,
                                        homeButtonsConfig = homeButtonsConfig,
                                        onStartGame = {
                                            viewModel.requestPlay()
                                        },
                                        onNavigate = { target -> currentRoute = target }
                                    )
                                }
                            }

                            appUpdateConfig?.let { config ->
                                com.example.ui.components.UpdateDialog(
                                    latestVersion = config.latestVersion,
                                    downloadUrl = config.downloadUrl,
                                    forceUpdate = config.forceUpdate,
                                    onDismiss = { viewModel.appUpdateConfig.value = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
