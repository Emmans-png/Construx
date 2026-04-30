package com.collins.todo.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.collins.todo.data.Models.Todo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onAddTodo: () -> Unit,
    onEditTodo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val todoList by homeViewModel.todoList
    val isLoading by homeViewModel.isLoading

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Tasks") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTodo) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        },
        modifier = modifier
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(todoList) { todo ->
                    TodoItem(
                        todo = todo,
                        onToggleComplete = { homeViewModel.toggleComplete(todo) },
                        onDelete = { todo.id?.let { homeViewModel.deleteTask(it) } },
                        onClick = { todo.id?.let { onEditTodo(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = todo.isComplete,
                    onCheckedChange = { onToggleComplete() }
                )
                Column {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (todo.isComplete) TextDecoration.LineThrough else null
                    )
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
