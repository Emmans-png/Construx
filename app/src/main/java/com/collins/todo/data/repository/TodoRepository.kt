package com.collins.todo.data.repository

import com.collins.todo.data.Models.Todo
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from

class TodoRepository : TodoService {
    val supabase = SupabaseClient.client

    override suspend fun createTask(todo: Todo): Todo? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        val todoWithUser = todo.copy(userId = user.id)
        val task = supabase.from("tasks").insert(todoWithUser) {
            select()
        }.decodeSingleOrNull<Todo>()
        return task
    }

    override suspend fun getAllTasks(): List<Todo> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        val tasks = supabase.from("tasks").select {
            filter {
                eq("userId", user.id)
            }
        }.decodeList<Todo>()
        return tasks
    }

    override suspend fun getTask(id: Int): Todo? {
        val todo = supabase.from("tasks").select {
            filter {
                eq("id", id)
            }
        }.decodeAsOrNull<Todo>()
        return todo
    }

    override suspend fun updateTask(todo: Todo): Todo? {
        val updatedTodo = supabase.from("tasks").update(todo) {
            select()
            filter {
                eq("id", todo.id!!)
            }
        }.decodeSingleOrNull<Todo>()
        return updatedTodo
    }

    override suspend fun deleteTask(id: Int): Boolean {
        supabase.from("tasks").delete {
            filter {
                eq("id", id)
            }
        }
        return getTask(id) == null
    }
}
