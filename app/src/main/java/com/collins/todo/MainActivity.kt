package com.collins.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.collins.todo.ui.theme.ToDoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoTheme {
                TodoApp()
            }
        }
    }
}

@Composable
fun TodoApp() {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    homeViewModel = homeViewModel,
                    onAddTodo = { navController.navigate("todo_form") },
                    onEditTodo = { id -> navController.navigate("todo_form?id=$id") }
                )
            }
            composable(
                route = "todo_form?id={id}",
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id")
                val todoId = if (id == -1) null else id
                val todoViewModel: TodoVIewModel = viewModel()
                
                TodoForm(
                    viewModel = todoViewModel,
                    todoId = todoId,
                    onSaveSuccess = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
