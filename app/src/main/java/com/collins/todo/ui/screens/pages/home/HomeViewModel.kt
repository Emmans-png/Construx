package com.collins.todo.ui.screens.pages.home

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.ConstructionProject
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.repository.ConstructionRepository
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {
    private val repository = ConstructionRepository()

    private val _projects = mutableStateOf<List<ConstructionProject>>(emptyList())
    val projects: State<List<ConstructionProject>> = _projects

    private val _orders = mutableStateOf<List<MaterialOrder>>(emptyList())
    val orders: State<List<MaterialOrder>> = _orders

    private val _drivers = mutableStateOf<List<com.collins.todo.data.Models.UserProfile>>(emptyList())
    val drivers: State<List<com.collins.todo.data.Models.UserProfile>> = _drivers

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _unreadMessageCount = mutableStateOf(0)
    val unreadMessageCount: State<Int> = _unreadMessageCount

    // UI State for Dialogs
    var showAddOrderDialog by mutableStateOf(false)
    var selectedProjectIdForOrder by mutableStateOf<Int?>(null)
    var editingOrder by mutableStateOf<MaterialOrder?>(null)

    init {
        refreshAll()
        fetchUnreadCount()
        setupRealtime()
    }

    private fun setupRealtime() {
        val projectChannel = SupabaseClient.client.channel("projects_sync")
        projectChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "construction_projects"
        }.onEach {
            fetchProjects()
        }.launchIn(viewModelScope)

        val orderChannel = SupabaseClient.client.channel("orders_sync")
        orderChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "material_orders"
        }.onEach {
            fetchOrders()
        }.launchIn(viewModelScope)

        val profileChannel = SupabaseClient.client.channel("profiles_sync")
        profileChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "profiles"
        }.onEach {
            fetchDrivers()
        }.launchIn(viewModelScope)

        val messageChannel = SupabaseClient.client.channel("home_messages_sync")
        messageChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }.onEach {
            fetchUnreadCount()
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            projectChannel.subscribe()
            orderChannel.subscribe()
            profileChannel.subscribe()
            messageChannel.subscribe()
        }
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile() ?: return@launch
                val messages = repository.getMessagesForUser()
                _unreadMessageCount.value = messages.count { it.receiverId == profile.id && !it.isRead }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearUnreadCount() {
        _unreadMessageCount.value = 0
    }

    fun refreshAll() {
        fetchProjects()
        fetchOrders()
        fetchDrivers()
    }

    fun fetchDrivers() {
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile()
                val orgId = profile?.organizationId
                if (!orgId.isNullOrBlank()) {
                    _drivers.value = repository.getDriversByOrganization(orgId)
                } else {
                    // Fallback to show all transporters if organization is not set yet
                    _drivers.value = repository.getAllDrivers()
                }
                println("DEBUG: Found ${_drivers.value.size} drivers")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchOrders() {
        viewModelScope.launch {
            try {
                // Fetch projects first to get IDs, then fetch orders for those projects
                val userProjects = repository.getProjects()
                val allOrders = mutableListOf<MaterialOrder>()
                userProjects.forEach { project ->
                    project.id?.let { id ->
                        allOrders.addAll(repository.getOrdersByProject(id))
                    }
                }
                _orders.value = allOrders.sortedByDescending { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _projects.value = repository.getProjects()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProject(projectId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteProject(projectId)
                fetchProjects() // Refresh list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
