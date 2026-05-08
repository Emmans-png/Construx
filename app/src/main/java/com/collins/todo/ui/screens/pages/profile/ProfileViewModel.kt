package com.collins.todo.ui.screens.pages.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.UserProfile
import com.collins.todo.data.repository.ConstructionRepository
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = ConstructionRepository()

    private val _profile = mutableStateOf<UserProfile?>(null)
    val profile: State<UserProfile?> = _profile

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val userProfile = repository.getUserProfile()
                _profile.value = userProfile
                if (userProfile == null) {
                    println("Profile fetch returned null for user.")
                } else {
                    println("Profile fetched successfully: ${userProfile.username}")
                }
            } catch (e: Exception) {
                println("Error fetching profile: ${e.message}")
                _errorMessage.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(updatedProfile: UserProfile, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                println("Updating profile for: ${updatedProfile.id}")
                val result = repository.updateUserProfile(updatedProfile)
                if (result != null) {
                    _profile.value = result
                    println("Profile updated successfully in Supabase.")
                    onSuccess()
                } else {
                    println("Profile update returned null.")
                    _errorMessage.value = "Failed to update profile (no result returned)"
                }
            } catch (e: Exception) {
                println("Error updating profile: ${e.message}")
                _errorMessage.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                if (user != null) {
                    repository.deleteUserProfile(user.id)
                    SupabaseClient.client.auth.signOut()
                    onSuccess()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete account"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
