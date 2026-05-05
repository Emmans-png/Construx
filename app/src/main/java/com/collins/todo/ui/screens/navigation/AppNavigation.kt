package com.collins.todo.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.collins.todo.ui.screens.home.HomeScreen
import com.collins.todo.ui.screens.home.HomeViewModel
import com.collins.todo.ui.screens.todoform.TodoForm
import com.collins.todo.ui.screens.todoform.TodoVIewModel
import com.collins.todo.ui.screens.login.LoginScreen
import com.collins.todo.ui.screens.login.AuthViewModel
import com.collins.todo.ui.screens.signup.RegisterScreen
import com.collins.todo.ui.screens.forgotpassword.ForgotScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = ROUTES.LOGIN) {
        composable(ROUTES.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(ROUTES.REGISTER)
                },
                onNavigateToForgot = {
                    navController.navigate(ROUTES.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
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
                    navController.popBackStack()
                },
                onSignUpSuccess = {
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
                    navController.popBackStack()
                }
            )
        }
        composable(ROUTES.HOME) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                homeViewModel = homeViewModel,
                onAddTodo = {
                    navController.navigate(ROUTES.createTodoFormRoute())
                },
                onEditTodo = { todoId ->
                    navController.navigate(ROUTES.createTodoFormRoute(todoId))
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
            val todoFormViewModel: TodoVIewModel = viewModel()
            TodoForm(
                viewModel = todoFormViewModel,
                todoId = todoId,
                onSaveSuccess = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
