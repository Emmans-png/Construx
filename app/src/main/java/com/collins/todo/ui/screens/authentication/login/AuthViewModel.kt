package com.collins.todo.ui.screens.authentication.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.UserProfile
import com.collins.todo.data.repository.ConstructionRepository
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
    var selectedSiteId by mutableStateOf<Int?>(null)
    var availableSites by mutableStateOf<List<com.collins.todo.data.Models.ConstructionProject>>(emptyList())
    var availableOrganizations by mutableStateOf<List<Pair<String, String>>>(emptyList()) // List of Pair(Name, ID)
    var username by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)

    private val _currentUserProfile = mutableStateOf<UserProfile?>(null)
    val currentUserProfile: UserProfile? get() = _currentUserProfile.value

    init {
        fetchAvailableSites()
        fetchAvailableOrganizations()
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            val user = SupabaseClient.client.auth.currentUserOrNull()
            if (user != null) {
                _currentUserProfile.value = ConstructionRepository().getUserProfile()
            }
        }
    }

    fun fetchAvailableOrganizations() {
        viewModelScope.launch {
            try {
                // Fetch from the dedicated organizations table
                val result = SupabaseClient.client.from("organizations")
                    .select().decodeList<com.collins.todo.data.Models.Organization>()
                
                availableOrganizations = result
                    .map { it.name to it.id }
                
                println("Fetched Organizations: ${availableOrganizations.size}")
            } catch (e: Exception) {
                println("Error fetching organizations: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun fetchAvailableSites() {
        viewModelScope.launch {
            try {
                // Fetch all sites/projects for selection
                availableSites = com.collins.todo.data.repository.SupabaseClient.client.from("construction_projects").select().decodeList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
                // 1. Create Auth User
                println("Attempting Signup for: $trimmedEmail")
                SupabaseClient.client.auth.signUpWith(Email) {
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
                        put("preferred_site_id", selectedSiteId)
                    }
                }
                
                // 2. Wait for session or user to be available
                val user = SupabaseClient.client.auth.currentUserOrNull()
                
                if (user != null) {
                    println("Auth Success. User ID: ${user.id}. Syncing profile...")
                    // 3. Sync to profiles table
                    try {
                        SupabaseClient.client.from("profiles").upsert(
                            buildJsonObject {
                                put("id", user.id)
                                put("username", trimmedUsername)
                                put("email", trimmedEmail)
                                put("organization_name", organizationName.trim())
                                put("organization_id", organizationId)
                                put("industry_type", industryType)
                                put("location", location.trim())
                                put("role", role)
                                put("phone_number", phoneNumber.trim())
                                put("preferred_site_id", selectedSiteId)
                            }
                        )
                        println("Profile Sync Complete.")
                        isSuccess = true
                        onSuccess()
                    } catch (e: Exception) {
                        println("Profile Sync Failure: ${e.message}")
                        // Still treat as success if auth passed, or show warning
                        isSuccess = true
                        onSuccess()
                    }
                } else {
                    // This happens if email confirmation is enabled in Supabase
                    println("Auth signup initiated. Please check email if confirmation is enabled.")
                    isSuccess = true
                    errorMessage = "Signup successful! Please check your email for confirmation (if enabled)."
                    onSuccess()
                }

            } catch (e: Exception) {
                println("Signup Error: ${e.message}")
                e.printStackTrace()
                val msg = e.message ?: ""
                errorMessage = when {
                    msg.contains("user_already_exists", ignoreCase = true) -> "An account with this email already exists"
                    msg.contains("weak_password", ignoreCase = true) -> "Password is too weak"
                    else -> "Signup failed: ${e.localizedMessage}"
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