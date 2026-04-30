package com.collins.todo.ui.screens.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.Todo
import com.collins.todo.data.repository.TodoRepository
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {
    private val repository = TodoRepository()

    private val _todoList = mutableStateOf<List<Todo>>(emptyList())
    val todoList: State<List<Todo>> = _todoList

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        fetchTasks()
    }

    fun fetchTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _todoList.value = repository.getAllTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            try {
                val success = repository.deleteTask(id)
                if (success) {
                    fetchTasks()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleComplete(todo: Todo) {
        viewModelScope.launch {
            try {
                repository.updateTask(todo.copy(isComplete = !todo.isComplete))
                fetchTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}