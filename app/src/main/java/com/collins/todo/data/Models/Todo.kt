package com.collins.todo.data.Models

data class Todo (
    val id:Int? =null, // will be used a a Primary key in supabase
    val createdAt:Long? = null,  // wil be automatically set to now() everytime we do a creation
    val title:String,
    val description:String,
    val media: String, //  store images or video
    val isComplete: Boolean = false, // default to false
    val dueDate:Long, // store as unix timestamp

)

