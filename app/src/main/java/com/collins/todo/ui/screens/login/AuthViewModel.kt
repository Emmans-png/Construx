package com.collins.todo.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = this@AuthViewModel.email
                    this.password = this@AuthViewModel.password
                }
                onSuccess()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Login failed"
            } finally {
                isLoading = false
            }
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = this@AuthViewModel.email
                    this.password = this@AuthViewModel.password
                }
                isSuccess = true
                onSuccess()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Signup failed"
            } finally {
                isLoading = false
            }
        }
    }

    fun resetPassword() {
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
}