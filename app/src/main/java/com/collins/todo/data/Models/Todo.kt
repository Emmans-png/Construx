package com.collins.todo.data.Models

import kotlinx.serialization.Serializable

@Serializable
data class Todo (
    val id: Int? = null,
    val createdAt: Long? = null,
    val title: String,
    val description: String,
    val media: String,
    val isComplete: Boolean = false,
    val dueDate: Long
)
