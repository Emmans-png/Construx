package com.collins.todo.ui.screens.todo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.collins.todo.data.Models.Todo

class TodoViewModel : ViewModel() {
    // State
    private val _todo = MutableLiveData<Todo>()
    val todo: LiveData<Todo> = _todo

    // Methods
    fun setTodo(todo: Todo) {
        _todo.value = todo
    }
}
