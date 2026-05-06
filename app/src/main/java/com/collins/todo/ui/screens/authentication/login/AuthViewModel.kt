package com.collins.todo.ui.screens.authentication.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthViewModel : ViewModel() {
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)

    fun login(onSuccess: () -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            errorMessage = "Fields cannot be empty"
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = trimmedEmail
                    this.password = trimmedPassword
                }
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: ""
                errorMessage = when {
                    msg.contains("invalid_credentials", ignoreCase = true) -> "Invalid email or password"
                    msg.contains("email_not_confirmed", ignoreCase = true) -> "Please confirm your email first"
                    msg.contains("rate_limit", ignoreCase = true) -> "Too many attempts. Please try again later."
                    else -> "Login failed. Please check your credentials."
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (trimmedUsername.isBlank() || trimmedEmail.isBlank() || trimmedPassword.isBlank() || confirmPassword.isBlank()) {
            errorMessage = "Fields cannot be empty"
            return
        }
        if (trimmedPassword != confirmPassword.trim()) {
            errorMessage = "Passwords do not match"
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = trimmedEmail
                    this.password = trimmedPassword
                    this.data = buildJsonObject {
                        put("username", trimmedUsername)
                    }
                }
                isSuccess = true
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                if (session != null) {
                    onSuccess()
                } else {
                    errorMessage = "Signup successful! Please check your email to confirm your account."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: ""
                errorMessage = when {
                    msg.contains("user_already_exists", ignoreCase = true) -> "An account with this email already exists"
                    msg.contains("weak_password", ignoreCase = true) -> "Password is too weak"
                    else -> "Signup failed. Please try again."
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun resetPassword() {
        if (email.isBlank()) {
            errorMessage = "Email cannot be empty"
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(email)
                isSuccess = true
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Reset failed"
            } finally {
                isLoading = false
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
                clearState()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearState() {
        errorMessage = null
        isSuccess = false
        isLoading = false
        username = ""
        email = ""
        password = ""
        confirmPassword = ""
    }
}