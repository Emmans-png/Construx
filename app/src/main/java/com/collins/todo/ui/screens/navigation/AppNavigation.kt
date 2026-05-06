package com.collins.todo.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.collins.todo.data.repository.SupabaseClient
import com.collins.todo.ui.screens.pages.home.HomeScreen
import com.collins.todo.ui.screens.pages.home.HomeViewModel
import com.collins.todo.ui.screens.pages.about.AboutScreen
import com.collins.todo.ui.screens.pages.contact.ContactScreen
import com.collins.todo.ui.screens.todoform.TodoForm
import com.collins.todo.ui.screens.todoform.TodoViewModel
import com.collins.todo.ui.screens.authentication.login.LoginScreen
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import com.collins.todo.ui.screens.authentication.signup.RegisterScreen
import com.collins.todo.ui.screens.authentication.forgotpassword.ForgotScreen
import io.github.jan.supabase.auth.auth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    LaunchedEffect(Unit) {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        if (session != null) {
            navController.navigate(ROUTES.HOME) {
                popUpTo(ROUTES.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = ROUTES.LOGIN) {
        composable(ROUTES.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.clearState()
                    navController.navigate(ROUTES.REGISTER)
                },
                onNavigateToForgot = {
                    authViewModel.clearState()
                    navController.navigate(ROUTES.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    authViewModel.clearState()
                    navController.navigate(ROUTES.HOME) {
                        popUpTo(ROUTES.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTES.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.clearState()
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    authViewModel.clearState()
                    navController.navigate(ROUTES.HOME) {
                        popUpTo(ROUTES.LOGIN) { inclusive = true }
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
            HomeScreen(
                homeViewModel = homeViewModel,
                authViewModel = authViewModel,
                onAddTodo = {
                    navController.navigate(ROUTES.createTodoFormRoute())
                },
                onEditTodo = { todoId ->
                    navController.navigate(ROUTES.createTodoFormRoute(todoId))
                },
                onNavigateToAbout = {
                    navController.navigate(ROUTES.ABOUT)
                },
                onNavigateToContact = {
                    navController.navigate(ROUTES.CONTACT)
                },
                onLogout = {
                    navController.navigate(ROUTES.LOGIN) {
                        popUpTo(ROUTES.HOME) { inclusive = true }
                    }
                },
                onRefresh = {
                    homeViewModel.fetchTasks()
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
