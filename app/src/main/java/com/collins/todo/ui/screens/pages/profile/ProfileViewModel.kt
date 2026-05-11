package com.collins.todo.ui.screens.pages.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.UserProfile
import com.collins.todo.data.repository.ConstructionRepository
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

class ProfileViewModel : ViewModel() {
    private val repository = ConstructionRepository()
    private val supabaseStorage = SupabaseClient.client.storage

    private val _profile = mutableStateOf<UserProfile?>(null)
    val profile: State<UserProfile?> = _profile

    private val _stats = mutableStateOf<Map<String, String>>(emptyMap())
    val stats: State<Map<String, String>> = _stats

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    // UI State for Editing
    var isEditing by mutableStateOf(false)
    var username by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var organizationName by mutableStateOf("")
    var location by mutableStateOf("")
    var vehiclePlate by mutableStateOf("")
    var vehicleModel by mutableStateOf("")
    var selectedImageUri by mutableStateOf<Uri?>(null)
    var showDeleteDialog by mutableStateOf(false)

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
                if (userProfile != null) {
                    fetchStats(userProfile)
                    println("Profile fetched successfully: ${userProfile.username}")
                } else {
                    println("Profile fetch returned null for user.")
                }
            } catch (e: Exception) {
                println("Error fetching profile: ${e.message}")
                _errorMessage.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchStats(userProfile: UserProfile) {
        viewModelScope.launch {
            try {
                if (userProfile.role == "Manager") {
                    val projects = repository.getProjects()
                    val team = repository.getTeam()
                    _stats.value = mapOf(
                        "Projects" to projects.size.toString(),
                        "Team Size" to team.size.toString(),
                        "Total Budget" to "$${String.format(Locale.getDefault(), "%,.0f", projects.sumOf { it.totalBudget })}"
                    )
                } else {
                    val orders = repository.getTransporterOrders()
                    _stats.value = mapOf(
                        "Deliveries" to orders.size.toString(),
                        "Completed" to orders.count { it.status == "Delivered" || it.status == "Completed" }.toString(),
                        "Active Trips" to orders.count { it.status == "Ongoing" || it.status == "Dispatched" }.toString()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                // Extract a cleaner error message if possible
                val cleanMessage = e.message?.lineSequence()?.firstOrNull() ?: "Unknown error"
                _errorMessage.value = "Update failed: $cleanMessage"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadProfilePicture(context: Context, imageUri: Uri, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                }

                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val data = byteArrayOutputStream.toByteArray()

                val fileName = "${_profile.value?.id ?: UUID.randomUUID()}.jpg"
                val path = "avatars/$fileName"

                supabaseStorage.from("profile_pictures").upload(path, data) {
                    upsert = true
                }
                val publicUrl = supabaseStorage.from("profile_pictures").publicUrl(path)

                _profile.value?.let { currentProfile ->
                    val updatedProfile = currentProfile.copy(profilePictureUrl = publicUrl)
                    updateProfile(updatedProfile) {
                        onSuccess(true)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upload image: ${e.message}"
                onSuccess(false)
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
