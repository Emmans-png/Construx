package com.collins.todo.ui.screens.todoform

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoForm(
    viewModel: TodoVIewModel,
    todoId: Int? = null,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title by viewModel.title
    val description by viewModel.description
    val isSaving by viewModel.isSaving
    val saveSuccess by viewModel.saveSuccess

    LaunchedEffect(todoId) {
        if (todoId != null) {
            viewModel.loadTodo(todoId)
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (todoId == null) "Add Task" else "Edit Task") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Cancel")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )

            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isSaving
            )

            // Simplification: Due date picker could be added here
            
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveTodo(todoId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && title.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        }
    }
}
