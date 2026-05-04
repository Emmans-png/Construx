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

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TodoForm : Screen("todo_form?todoId={todoId}") {
        fun createRoute(todoId: Int? = null) = if (todoId != null) "todo_form?todoId=$todoId" else "todo_form"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                homeViewModel = homeViewModel,
                onAddTodo = {
                    navController.navigate(Screen.TodoForm.createRoute())
                },
                onEditTodo = { todoId ->
                    navController.navigate(Screen.TodoForm.createRoute(todoId))
                }
            )
        }
        composable(
            route = Screen.TodoForm.route,
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
