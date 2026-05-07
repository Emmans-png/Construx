package com.collins.todo.ui.screens.navigation

object ROUTES {
    const val LOGIN = "login"
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val ORGANIZATION_ENTRY = "organization_entry"
    const val ROLE_SELECTION = "role_selection"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val TRANSPORTER_HOME = "transporter_home"
    const val PROCUREMENT = "procurement"
    const val ROI_ANALYZER = "roi_analyzer"
    const val ABOUT = "about"
    const val CONTACT = "contact"
    const val PROJECT_FORM = "project_form?projectId={projectId}"
    const val TODO_FORM = "todo_form?todoId={todoId}"
    
    fun createProjectFormRoute(projectId: Int? = null) = 
        if (projectId != null) "project_form?projectId=$projectId" else "project_form"

    fun createTodoFormRoute(todoId: Int? = null) = 
        if (todoId != null) "todo_form?todoId=$todoId" else "todo_form"
}