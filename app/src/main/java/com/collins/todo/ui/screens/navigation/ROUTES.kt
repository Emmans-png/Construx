package com.collins.todo.ui.screens.navigation

object ROUTES {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val ABOUT = "about"
    const val CONTACT = "contact"
    const val TODO_FORM = "todo_form?todoId={todoId}"
    
    fun createTodoFormRoute(todoId: Int? = null) = 
        if (todoId != null) "todo_form?todoId=$todoId" else "todo_form"
}