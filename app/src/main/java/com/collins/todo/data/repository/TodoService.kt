package com.collins.todo.data.repository

import com.collins.todo.data.Models.Todo

interface TodoService {
        suspend fun createTask(todo: Todo): Todo? // create task
        suspend fun getAllTasks(): List<Todo> // read all tasks
        suspend fun getTask(id:Int): Todo? // read one task
        suspend fun updateTask(todo: Todo):Todo? // update task
        suspend fun deleteTask(id:Int): Boolean // delete task and return true or false based on success
    }

