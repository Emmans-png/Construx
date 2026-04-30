package com.collins.todo.ui.screens.todoform

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.Todo
import com.collins.todo.data.repository.TodoRepository
import kotlinx.coroutines.launch

class TodoVIewModel : ViewModel() {
    private val repository = TodoRepository()

    private val _title = mutableStateOf("")
    val title: State<String> = _title

    private val _description = mutableStateOf("")
    val description: State<String> = _description

    private val _dueDate = mutableStateOf(System.currentTimeMillis())
    val dueDate: State<Long> = _dueDate

    private val _isSaving = mutableStateOf(false)
    val isSaving: State<Boolean> = _isSaving

    private val _saveSuccess = mutableStateOf(false)
    val saveSuccess: State<Boolean> = _saveSuccess

    fun onTitleChange(newTitle: String) {
        _title.value = newTitle
    }

    fun onDescriptionChange(newDescription: String) {
        _description.value = newDescription
    }

    fun onDueDateChange(newDate: Long) {
        _dueDate.value = newDate
    }

    fun saveTodo(id: Int? = null) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val todo = Todo(
                    id = id,
                    title = _title.value,
                    description = _description.value,
                    media = "", // Placeholder for media
                    dueDate = _dueDate.value,
                    isComplete = false
                )
                if (id == null) {
                    repository.createTask(todo)
                } else {
                    repository.updateTask(todo)
                }
                _saveSuccess.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun loadTodo(id: Int) {
        viewModelScope.launch {
            try {
                val todo = repository.getTask(id)
                todo?.let {
                    _title.value = it.title
                    _description.value = it.description
                    _dueDate.value = it.dueDate
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}