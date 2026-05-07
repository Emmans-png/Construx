package com.collins.todo.ui.screens.authentication.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthViewModel : ViewModel() {
    var organizationName by mutableStateOf("")
    var organizationId by mutableStateOf("")
    var industryType by mutableStateOf("Shelter/Construction")
    var location by mutableStateOf("")
    var role by mutableStateOf("") // "Transporter" or "Manager"
    var username by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
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
                // Sign up in Auth
                val authResponse = SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = trimmedEmail
                    this.password = trimmedPassword
                    this.data = buildJsonObject {
                        put("username", trimmedUsername)
                        put("organization_name", organizationName.trim())
                        put("organization_id", organizationId)
                        put("industry_type", industryType)
                        put("location", location.trim())
                        put("role", role)
                        put("phone_number", phoneNumber.trim())
                    }
                }
                
                isSuccess = true
                val userId = authResponse?.id // Get ID directly from response
                
                if (userId != null) {
                    // Try to sync to profiles table
                    try {
                        SupabaseClient.client.from("profiles").upsert(
                            buildJsonObject {
                                put("id", userId)
                                put("username", trimmedUsername)
                                put("email", trimmedEmail)
                                put("organization_name", organizationName.trim())
                                put("organization_id", organizationId)
                                put("industry_type", industryType)
                                put("location", location.trim())
                                put("role", role)
                                put("phone_number", phoneNumber.trim())
                            }
                        )
                    } catch (e: Exception) {
                        println("Profile Sync Error: ${e.message}")
                        e.printStackTrace()
                    }
                }

                onSuccess()
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
        organizationName = ""
        organizationId = ""
        industryType = "Shelter/Construction"
        location = ""
        role = ""
        username = ""
        phoneNumber = ""
        email = ""
        password = ""
        confirmPassword = ""
    }

    fun getUserRole(): String {
        // Try to get role from local state first (set during signup)
        if (role.isNotBlank()) return role
        
        // Fallback to user metadata
        val user = SupabaseClient.client.auth.currentUserOrNull()
        return user?.userMetadata?.get("role")?.toString()?.removeSurrounding("\"") ?: "Manager"
    }
}