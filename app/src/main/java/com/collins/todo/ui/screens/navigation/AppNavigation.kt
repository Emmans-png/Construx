package com.collins.todo.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.collins.todo.ui.screens.authentication.signup.OrganizationEntryScreen
import com.collins.todo.ui.screens.authentication.signup.RoleSelectionScreen
import com.collins.todo.ui.screens.authentication.forgotpassword.ForgotScreen
import com.collins.todo.ui.screens.onboarding.OnboardingScreen
import com.collins.todo.ui.screens.onboarding.SplashScreen
import com.collins.todo.ui.screens.pages.manager.ManagerDashboard
import com.collins.todo.ui.screens.pages.transporter.TransporterConsole
import com.collins.todo.ui.screens.pages.transporter.TransporterViewModel
import com.collins.todo.ui.screens.pages.team.TeamScreen
import com.collins.todo.ui.screens.pages.team.TeamViewModel
import io.github.jan.supabase.auth.auth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = ROUTES.SPLASH) {
        composable(ROUTES.SPLASH) {
            SplashScreen(
                onFinished = { isLoggedIn ->
                    if (isLoggedIn) {
                        val role = authViewModel.getUserRole()
                        val destination = if (role == "Transporter") ROUTES.TRANSPORTER_HOME else ROUTES.HOME
                        navController.navigate(destination) {
                            popUpTo(ROUTES.SPLASH) { inclusive = true }
                        }
                    } else {
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
                    navController.navigate(ROUTES.ORGANIZATION_ENTRY) {
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
                    navController.navigate(ROUTES.ROLE_SELECTION)
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
                    navController.navigate(ROUTES.REGISTER)
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
                    val role = authViewModel.getUserRole()
                    val destination = if (role == "Transporter") ROUTES.TRANSPORTER_HOME else ROUTES.HOME
                    authViewModel.clearState()
                    navController.navigate(destination) {
                        popUpTo(ROUTES.SPLASH) { inclusive = true }
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
                onNavigateToAnalytics = { navController.navigate(ROUTES.ROI_ANALYZER) }
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
                }
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
            val homeEntry = remember { navController.getBackStackEntry(ROUTES.HOME) }
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
