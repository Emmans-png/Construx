package com.collins.todo.ui.screens.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.collins.todo.data.repository.SupabaseClient
import com.collins.todo.ui.screens.pages.home.HomeScreen
import com.collins.todo.ui.screens.pages.home.HomeViewModel
import com.collins.todo.ui.screens.pages.home.ProjectFormScreen
import com.collins.todo.ui.screens.pages.about.AboutScreen
import com.collins.todo.ui.screens.pages.contact.ContactScreen
import com.collins.todo.ui.screens.pages.procurement.ProcurementScreen
import com.collins.todo.ui.screens.pages.roi.ROIAnalyzerScreen
import com.collins.todo.ui.screens.todoform.TodoForm
import com.collins.todo.ui.screens.todoform.TodoViewModel
import com.collins.todo.ui.screens.authentication.login.LoginScreen
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import com.collins.todo.ui.screens.authentication.signup.RegisterScreen
import com.collins.todo.ui.screens.authentication.signup.SignupSuccessScreen
import com.collins.todo.ui.screens.authentication.signup.OrganizationEntryScreen
import com.collins.todo.ui.screens.authentication.signup.RoleSelectionScreen
import com.collins.todo.ui.screens.authentication.forgotpassword.ForgotScreen
import com.collins.todo.ui.screens.onboarding.OnboardingScreen
import com.collins.todo.ui.screens.onboarding.SplashScreen
import com.collins.todo.ui.screens.pages.manager.ManagerDashboard
import com.collins.todo.ui.screens.pages.transporter.TransporterConsole
import com.collins.todo.ui.screens.pages.transporter.TransporterViewModel
import com.collins.todo.ui.screens.pages.transporter.TrackingScreen
import com.collins.todo.ui.screens.pages.transporter.TrackingViewModel
import com.collins.todo.ui.screens.pages.profile.ProfileScreen
import com.collins.todo.ui.screens.pages.profile.ProfileViewModel
import com.collins.todo.ui.screens.pages.team.TeamScreen
import com.collins.todo.ui.screens.pages.team.TeamViewModel
import io.github.jan.supabase.auth.auth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = ROUTES.SPLASH) {
        composable(ROUTES.SPLASH) {
            SplashScreen(
                onFinished = { isLoggedIn, _ ->
                    if (isLoggedIn) {
                        val role = authViewModel.getUserRole()
                        val destination = if (role == "Transporter") ROUTES.TRANSPORTER_HOME else ROUTES.HOME
                        navController.navigate(destination) {
                            popUpTo(ROUTES.SPLASH) { inclusive = true }
                        }
                    } else {
                        // Always go to LOGIN, skipping onboarding on launch
                        navController.navigate(ROUTES.LOGIN) {
                            popUpTo(ROUTES.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(ROUTES.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(ROUTES.ROLE_SELECTION) {
                        popUpTo(ROUTES.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTES.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.clearState()
                    // Start onboarding when clicking "Sign Up"
                    navController.navigate(ROUTES.ONBOARDING)
                },
                onNavigateToForgot = {
                    authViewModel.clearState()
                    navController.navigate(ROUTES.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    val role = authViewModel.getUserRole()
                    val destination = if (role == "Transporter") ROUTES.TRANSPORTER_HOME else ROUTES.HOME
                    authViewModel.clearState()
                    navController.navigate(destination) {
                        popUpTo(ROUTES.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTES.ORGANIZATION_ENTRY) {
            OrganizationEntryScreen(
                viewModel = authViewModel,
                onNext = {
                    navController.navigate(ROUTES.REGISTER)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(ROUTES.ROLE_SELECTION) {
            RoleSelectionScreen(
                viewModel = authViewModel,
                onNext = {
                    if (authViewModel.role == "Transporter") {
                        navController.navigate(ROUTES.REGISTER)
                    } else {
                        navController.navigate(ROUTES.ORGANIZATION_ENTRY)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(ROUTES.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.clearState()
                    navController.navigate(ROUTES.LOGIN) {
                        popUpTo(ROUTES.ORGANIZATION_ENTRY) { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate(ROUTES.SIGNUP_SUCCESS) {
                        popUpTo(ROUTES.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTES.SIGNUP_SUCCESS) {
            SignupSuccessScreen(
                onContinue = {
                    val role = authViewModel.getUserRole()
                    val destination = if (role == "Transporter") ROUTES.TRANSPORTER_HOME else ROUTES.HOME
                    authViewModel.clearState()
                    navController.navigate(destination) {
                        popUpTo(ROUTES.SIGNUP_SUCCESS) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTES.FORGOT_PASSWORD) {
            ForgotScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    authViewModel.clearState()
                    navController.popBackStack()
                }
            )
        }
        composable(ROUTES.HOME) {
            val homeViewModel: HomeViewModel = viewModel()
            ManagerDashboard(
                viewModel = homeViewModel,
                authViewModel = authViewModel,
                onLogout = {
                    navController.navigate(ROUTES.LOGIN) {
                        popUpTo(ROUTES.HOME) { inclusive = true }
                    }
                },
                onNavigateToProjects = { /* Already here */ },
                onNavigateToMaterials = { navController.navigate(ROUTES.PROCUREMENT) },
                onNavigateToTeam = { navController.navigate("team") },
                onNavigateToAnalytics = { navController.navigate(ROUTES.ROI_ANALYZER) },
                onNavigateToProfile = { navController.navigate(ROUTES.PROFILE) },
                onAddProject = { navController.navigate(ROUTES.createProjectFormRoute()) },
                onEditProject = { project ->
                    navController.navigate(ROUTES.createProjectFormRoute(project.id))
                }
            )
        }
        composable("team") {
            val teamViewModel: TeamViewModel = viewModel()
            TeamScreen(
                viewModel = teamViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(ROUTES.TRANSPORTER_HOME) {
            val transporterViewModel: TransporterViewModel = viewModel()
            TransporterConsole(
                viewModel = transporterViewModel,
                authViewModel = authViewModel,
                onLogout = {
                    navController.navigate(ROUTES.LOGIN) {
                        popUpTo(ROUTES.TRANSPORTER_HOME) { inclusive = true }
                    }
                },
                onNavigateToTracking = { orderId ->
                    navController.navigate("${ROUTES.TRACKING}/$orderId/false")
                },
                onNavigateToProfile = { navController.navigate(ROUTES.PROFILE) }
            )
        }
        composable(ROUTES.PROFILE) {
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onDeleteAccount = {
                    navController.navigate(ROUTES.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "${ROUTES.TRACKING}/{orderId}/{isViewer}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType },
                navArgument("isViewer") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId")
            val isViewer = backStackEntry.arguments?.getBoolean("isViewer") ?: false
            val trackingViewModel: TrackingViewModel = viewModel()
            TrackingScreen(
                viewModel = trackingViewModel,
                onBack = { navController.popBackStack() },
                orderId = orderId,
                isViewer = isViewer
            )
        }
        composable(
            route = ROUTES.PROJECT_FORM,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getInt("projectId")?.takeIf { it != -1 }
            val homeEntry = remember(backStackEntry) { navController.getBackStackEntry(ROUTES.HOME) }
            val homeViewModel: HomeViewModel = viewModel(homeEntry)
            ProjectFormScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onSaveSuccess = {
                    homeViewModel.fetchProjects()
                    navController.popBackStack()
                }
            )
        }
        composable(ROUTES.ABOUT) {
            AboutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(ROUTES.CONTACT) {
            ContactScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(ROUTES.PROCUREMENT) {
            ProcurementScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToLiveTracking = { orderId ->
                    navController.navigate("${ROUTES.TRACKING}/$orderId/true")
                }
            )
        }
        composable(ROUTES.ROI_ANALYZER) {
            ROIAnalyzerScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = ROUTES.TODO_FORM,
            arguments = listOf(
                navArgument("todoId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val todoId = backStackEntry.arguments?.getInt("todoId")?.takeIf { it != -1 }
            val todoFormViewModel: TodoViewModel = viewModel()
            TodoForm(
                viewModel = todoFormViewModel,
                todoId = todoId,
                onSaveSuccess = {
                    todoFormViewModel.clearState()
                    navController.popBackStack()
                },
                onBack = {
                    todoFormViewModel.clearState()
                    navController.popBackStack()
                }
            )
        }
    }
}
