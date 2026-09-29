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
                val topUsersByMmr by viewModel.topUsersByMmr.collectAsStateWithLifecycle()
                val topUsersByXp by viewModel.topUsersByXp.collectAsStateWithLifecycle()
                val topUsersByWins by viewModel.topUsersByWins.collectAsStateWithLifecycle()
                val allTournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
                val allKnockoutTournaments by viewModel.allKnockoutTournaments.collectAsStateWithLifecycle()
                val notifications by viewModel.notifications.collectAsStateWithLifecycle()
                val unreadNotifsCount by viewModel.unreadNotifsCount.collectAsStateWithLifecycle()
                val allBadges by viewModel.allBadges.collectAsStateWithLifecycle()
                val selectedChatRoom by viewModel.selectedChatRoom.collectAsStateWithLifecycle()
                val chatMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()
                val gameState by viewModel.gameState.collectAsStateWithLifecycle()
                val withdrawSubmitting by viewModel.withdrawSubmitting.collectAsStateWithLifecycle()

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

                // Handle Back Button
                if (currentRoute != "home" && !gameState.active && !gameState.isGameOver) {
                    BackHandler {
                        when (currentRoute) {
                            "checkout" -> currentRoute = "deposit"
                            "payment_processing" -> currentRoute = "wallet"
                            "deposit", "add_money" -> currentRoute = "wallet"
                            "withdraw", "transactions" -> currentRoute = "wallet"
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
                                        onStartGame = {
                                            viewModel.startGame()
                                        },
                                        onNavigate = { target -> currentRoute = target }
                                    )

                                    "wallet" -> WalletScreen(
                                        user = currentUser,
                                        transactions = userTransactions,
                                        onNavigate = { target -> currentRoute = target },
                                        onRefreshWithdrawals = { viewModel.syncWithdrawals() }
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
                                        onRefreshWithdrawals = { viewModel.syncWithdrawals() },
                                        onBack = { currentRoute = "wallet" }
                                    )

                                    "transactions" -> TransactionsScreen(
                                        transactions = userTransactions,
                                        onBack = { currentRoute = "wallet" }
                                    )

                                    "leaderboard" -> LeaderboardScreen(
                                        currentUserId = currentUser?.id,
                                        topUsersByMmr = topUsersByMmr,
                                        topUsersByXp = topUsersByXp,
                                        topUsersByWins = topUsersByWins
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
                                        onPurchaseVip = { tier, price, days ->
                                            viewModel.purchaseVip(tier, price, days)
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

                                    "knockout" -> KnockoutScreen(
                                        tournaments = allKnockoutTournaments,
                                        onBack = { currentRoute = "home" }
                                    )

                                    "profile" -> ProfileScreen(
                                        user = currentUser,
                                        withdrawals = userWithdrawals,
                                        onUpdateProfile = { name, photo ->
                                            viewModel.updateProfile(name, photo)
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
                                        onStartGame = {
                                            viewModel.startGame()
                                        },
                                        onNavigate = { target -> currentRoute = target }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
