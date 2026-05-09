package com.collins.todo.ui.screens.pages.procurement

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.Message
import com.collins.todo.data.Models.UserProfile
import com.collins.todo.data.repository.ConstructionRepository
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ManagerMessagesViewModel : ViewModel() {
    private val repository = ConstructionRepository()
    
    private val _drivers = mutableStateOf<List<UserProfile>>(emptyList())
    val drivers: State<List<UserProfile>> = _drivers
    
    private val _selectedDriver = mutableStateOf<UserProfile?>(null)
    val selectedDriver: State<UserProfile?> = _selectedDriver
    
    private val _messages = mutableStateOf<List<Message>>(emptyList())
    val messages: State<List<Message>> = _messages
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    val currentUserId: String? = SupabaseClient.client.auth.currentUserOrNull()?.id

    init {
        fetchDrivers()
        setupRealtime()
    }

    private fun fetchDrivers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getUserProfile()
                profile?.organizationId?.let { orgId ->
                    _drivers.value = repository.getDriversByOrganization(orgId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDriver(driver: UserProfile?) {
        _selectedDriver.value = driver
        if (driver != null) {
            fetchMessages(driver.id)
            markAsRead(driver.id)
        } else {
            _messages.value = emptyList()
        }
    }

    private fun markAsRead(driverId: String) {
        viewModelScope.launch {
            repository.markMessagesAsRead(driverId)
        }
    }

    private fun fetchMessages(driverId: String) {
        viewModelScope.launch {
            try {
                val allMessages = repository.getMessagesForUser()
                _messages.value = allMessages.filter { 
                    (it.senderId == currentUserId && it.receiverId == driverId) || 
                    (it.senderId == driverId && it.receiverId == currentUserId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(content: String) {
        val receiverId = _selectedDriver.value?.id ?: return
        val senderId = currentUserId ?: return
        
        viewModelScope.launch {
            val success = repository.sendMessage(
                Message(
                    senderId = senderId,
                    receiverId = receiverId,
                    content = content
                )
            )
            if (success) {
                fetchMessages(receiverId)
            }
        }
    }

    private fun setupRealtime() {
        val channel = SupabaseClient.client.channel("manager_messages_sync")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }

        flow.onEach {
            _selectedDriver.value?.let { driver ->
                fetchMessages(driver.id)
                markAsRead(driver.id)
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            channel.subscribe()
        }
    }
}
