package com.collins.todo.data.Models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Todo (
    val id: Int? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("created_at")
    val createdAt: Long? = null,
    val title: String,
    val description: String,
    val media: String,
    @SerialName("is_complete")
    val isComplete: Boolean = false,
    @SerialName("due_date")
    val dueDate: Long
)
