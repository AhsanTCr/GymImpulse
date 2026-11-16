package com.example.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.database.AdminFirestoreRepository
import com.example.database.MemberFirestoreRepository
import com.example.database.UserProfileDoc
import com.example.database.toFriendlyFirestoreMessage
import com.example.features.admin.AdminPasscodeGate
import com.example.features.admin.AdminPortalRootScreen
import com.example.features.admin.AdminViewModel
import com.example.features.announcements.AnnouncementsScreen
import com.example.features.announcements.AnnouncementsViewModel
import com.example.features.attendance.QrAttendanceScreen
import com.example.features.attendance.QrAttendanceViewModel
import com.example.features.auth.AuthViewModel
import com.example.features.auth.LoginScreen
import com.example.features.auth.RegisterScreen
import com.example.features.leaderboard.LeaderboardScreen
import com.example.features.leaderboard.LeaderboardViewModel
import com.example.features.member.HomeDashboardViewModel
import com.example.features.member.MemberHomeScreen
import com.example.features.membership.MembershipViewModel
import com.example.features.membership.MyMembershipScreen
import com.example.features.notifications.NotificationsViewModel
import com.example.features.nutrition.DailyGoalsScreen
import com.example.features.nutrition.NutritionGoalsViewModel
import com.example.features.profile.AppUpdatesScreen
import com.example.features.profile.MemberProfileScreen
import com.example.features.profile.ProfileViewModel
import com.example.features.splash.SplashScreen
import com.example.services.GymAuthManager
import com.example.ui.components.ApexPulseLogo
import com.example.ui.theme.AppThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private sealed class ProfileGateState {
    data object Loading : ProfileGateState()
    data class Resolved(val profile: UserProfileDoc?) : ProfileGateState()
    data class Error(val message: String) : ProfileGateState()
}

@Composable
fun ApexPulseAppNavHost(
    currentThemeMode: AppThemeMode,
    onChangeThemeMode: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = remember(appContext) { MemberFirestoreRepository(appContext) }
    val adminRepository = remember(appContext) { AdminFirestoreRepository(appContext) }

    var splashFinished by remember { mutableStateOf(false) }
    val firebaseUser by GymAuthManager.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    // Attempt Credential Manager silent auto-sign-in once during app startup
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            GymAuthManager.attemptSilentSignIn(context)
        }
    }

    if (!splashFinished) {
        SplashScreen(
            onSplashFinished = { splashFinished = true },
            modifier = modifier
        )
        return
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(repository) }
        }
    )

    if (firebaseUser == null) {
        // Unauthenticated Root: strictly no authenticated ViewModels or Firestore listeners exist here
        val authNavController = rememberNavController()
        NavHost(
            navController = authNavController,
            startDestination = AppRoutes.ROUTE_LOGIN,
            modifier = modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) }
        ) {
            composable(AppRoutes.ROUTE_LOGIN) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = {
                        authNavController.navigate(AppRoutes.ROUTE_REGISTER)
                    }
                )
            }
            composable(AppRoutes.ROUTE_REGISTER) {
                RegisterScreen(
                    viewModel = authViewModel,
                    isAuthenticatedOnboarding = false,
                    onRegistrationSuccess = { /* Root auth state + profile gate handle transition */ },
                    onNavigateBack = { authNavController.popBackStack() }
                )
            }
        }
        return
    }

    val activeUser = firebaseUser!!
    val uid = activeUser.uid

    // Key the entire authenticated tree by uid so switching Google accounts resets all scoped ViewModels
    key(uid) {
        var retryNonce by remember { mutableStateOf(0) }
        val gateState by remember(uid, retryNonce) {
            repository.observeUserProfile(uid)
                .map<UserProfileDoc?, ProfileGateState> { ProfileGateState.Resolved(it) }
                .catch { error ->
                    emit(
                        ProfileGateState.Error(
                            error.toFriendlyFirestoreMessage("Unable to verify your member profile.")
                        )
                    )
                }
        }.collectAsStateWithLifecycle(initialValue = ProfileGateState.Loading)

        val isBackendAdminState by remember(uid) {
            adminRepository.observeIsAdminAuthorized(uid)
                .map<Boolean, Boolean?> { it }
                .catch { emit(false) }
        }.collectAsStateWithLifecycle(initialValue = null)

        when (val currentGate = gateState) {
            ProfileGateState.Loading -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ApexPulseLogo(size = 56.dp)
                        CircularProgressIndicator()
                        Text(
                            text = "Signing into GymPulse...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is ProfileGateState.Error -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Account Sync Issue",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currentGate.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { retryNonce++ }) {
                            Text("Retry Connection")
                        }
                        OutlinedButton(onClick = { authViewModel.signOut(context) }) {
                            Text("Sign Out")
                        }
                    }
                }
            }

            is ProfileGateState.Resolved -> {
                val resolvedAdmin = isBackendAdminState
                val profileDoc = currentGate.profile
                if (resolvedAdmin == null) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ApexPulseLogo(size = 56.dp)
                            CircularProgressIndicator()
                            Text(
                                text = "Verifying club access...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (profileDoc == null) {
                    LaunchedEffect(uid) {
                        authViewModel.prefillRegistrationFromUser(
                            displayName = activeUser.displayName.orEmpty(),
                            email = activeUser.email.orEmpty()
                        )
                    }
                    RegisterScreen(
                        viewModel = authViewModel,
                        isAuthenticatedOnboarding = true,
                        onRegistrationSuccess = { /* Snapshot listener advances automatically */ },
                        onNavigateBack = { authViewModel.signOut(context) },
                        modifier = modifier
                    )
                } else {
                    AuthenticatedMemberNavShell(
                        uid = uid,
                        userEmail = activeUser.email.orEmpty(),
                        isBackendAdminAuthorized = resolvedAdmin || profileDoc.role.equals("ADMIN", ignoreCase = true) && false,
                        repository = repository,
                        adminRepository = adminRepository,
                        authViewModel = authViewModel,
                        currentThemeMode = currentThemeMode,
                        onChangeThemeMode = onChangeThemeMode,
                        modifier = modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedMemberNavShell(
    uid: String,
    userEmail: String,
    isBackendAdminAuthorized: Boolean,
    repository: MemberFirestoreRepository,
    adminRepository: AdminFirestoreRepository,
    authViewModel: AuthViewModel,
    currentThemeMode: AppThemeMode,
    onChangeThemeMode: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in AppRoutes.bottomBarRoutes

    // Authenticated ViewModels strictly scoped to the verified Google user UID
    val homeViewModel: HomeDashboardViewModel = viewModel(
        key = "home_$uid",
        factory = viewModelFactory { initializer { HomeDashboardViewModel(repository, uid) } }
    )
    val leaderboardViewModel: LeaderboardViewModel = viewModel(
        key = "lb_$uid",
        factory = viewModelFactory { initializer { LeaderboardViewModel(repository, uid) } }
    )
    val qrAttendanceViewModel: QrAttendanceViewModel = viewModel(
        key = "qr_$uid",
        factory = viewModelFactory { initializer { QrAttendanceViewModel(repository, uid) } }
    )
    val membershipViewModel: MembershipViewModel = viewModel(
        key = "mem_$uid",
        factory = viewModelFactory { initializer { MembershipViewModel(repository, uid) } }
    )
    val nutritionViewModel: NutritionGoalsViewModel = viewModel(
        key = "nut_$uid",
        factory = viewModelFactory { initializer { NutritionGoalsViewModel(repository, uid) } }
    )
    val announcementsViewModel: AnnouncementsViewModel = viewModel(
        key = "ann_$uid",
        factory = viewModelFactory { initializer { AnnouncementsViewModel(repository) } }
    )
    val notificationsViewModel: NotificationsViewModel = viewModel(
        key = "notif_$uid",
        factory = viewModelFactory { initializer { NotificationsViewModel(repository, uid) } }
    )
    val profileViewModel: ProfileViewModel = viewModel(
        key = "prof_$uid",
        factory = viewModelFactory { initializer { ProfileViewModel(repository, uid) } }
    )

    fun navigateToMemberTab(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoutes.ROUTE_MEMBER_HOME) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = if (showBottomBar) WindowInsets.statusBars else WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(150))
            ) {
                MemberBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = ::navigateToMemberTab
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isBackendAdminAuthorized) AppRoutes.ROUTE_ADMIN_PORTAL else AppRoutes.ROUTE_MEMBER_HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) }
        ) {
            composable(AppRoutes.ROUTE_MEMBER_HOME) {
                MemberHomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToQrAttendance = { navigateToMemberTab(AppRoutes.ROUTE_QR_ATTENDANCE) },
                    onNavigateToMembership = { navController.navigate(AppRoutes.ROUTE_MY_MEMBERSHIP) },
                    onNavigateToGoals = { navigateToMemberTab(AppRoutes.ROUTE_DAILY_GOALS) },
                    onNavigateToLeaderboard = { navigateToMemberTab(AppRoutes.ROUTE_LEADERBOARD) },
                    onNavigateToAnnouncements = { navController.navigate(AppRoutes.ROUTE_ANNOUNCEMENTS) },
                    onNavigateToProfile = { navigateToMemberTab(AppRoutes.ROUTE_MEMBER_PROFILE) }
                )
            }

            composable(AppRoutes.ROUTE_LEADERBOARD) {
                LeaderboardScreen(viewModel = leaderboardViewModel)
            }

            composable(AppRoutes.ROUTE_QR_ATTENDANCE) {
                QrAttendanceScreen(viewModel = qrAttendanceViewModel)
            }

            composable(AppRoutes.ROUTE_DAILY_GOALS) {
                DailyGoalsScreen(viewModel = nutritionViewModel)
            }

            composable(AppRoutes.ROUTE_MEMBER_PROFILE) {
                MemberProfileScreen(
                    profileViewModel = profileViewModel,
                    notificationsViewModel = notificationsViewModel,
                    currentThemeMode = currentThemeMode,
                    onChangeThemeMode = onChangeThemeMode,
                    onNavigateToMembership = { navController.navigate(AppRoutes.ROUTE_MY_MEMBERSHIP) },
                    onNavigateToAppUpdates = { navController.navigate(AppRoutes.ROUTE_APP_UPDATES) },
                    isAdminAuthorized = isBackendAdminAuthorized,
                    onNavigateToAdminPreview = { navController.navigate(AppRoutes.ROUTE_ADMIN_PORTAL) },
                    onLogout = { authViewModel.signOut(context) }
                )
            }

            composable(AppRoutes.ROUTE_MY_MEMBERSHIP) {
                MyMembershipScreen(
                    viewModel = membershipViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_ANNOUNCEMENTS) {
                AnnouncementsScreen(
                    viewModel = announcementsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_APP_UPDATES) {
                AppUpdatesScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_ADMIN_PORTAL) {
                if (isBackendAdminAuthorized) {
                    val adminViewModel: AdminViewModel = viewModel(
                        key = "admin_$uid",
                        factory = viewModelFactory { initializer { AdminViewModel(adminRepository, uid) } }
                    )
                    AdminPortalRootScreen(
                        isBackendAdminAuthorized = true,
                        adminEmail = userEmail,
                        viewModel = adminViewModel,
                        onSwitchToMemberView = {
                            if (!navController.popBackStack(AppRoutes.ROUTE_MEMBER_HOME, inclusive = false)) {
                                navController.navigate(AppRoutes.ROUTE_MEMBER_HOME) {
                                    popUpTo(AppRoutes.ROUTE_ADMIN_PORTAL) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onLogout = { authViewModel.signOut(context) }
                    )
                } else {
                    AdminPasscodeGate(
                        isBackendAdminAuthorized = false,
                        adminEmail = userEmail,
                        passcodeError = null,
                        onUnlockWithPasscode = {},
                        onSwitchToMemberView = {
                            if (!navController.popBackStack(AppRoutes.ROUTE_MEMBER_HOME, inclusive = false)) {
                                navController.navigate(AppRoutes.ROUTE_MEMBER_HOME) {
                                    popUpTo(AppRoutes.ROUTE_ADMIN_PORTAL) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onLogout = { authViewModel.signOut(context) }
                    )
                }
            }
        }
    }
}
