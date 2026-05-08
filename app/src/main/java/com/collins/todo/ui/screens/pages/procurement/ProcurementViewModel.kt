package com.collins.todo.ui.screens.pages.procurement

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.ConstructionProject
import com.collins.todo.data.Models.MaterialOrder
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

class ProcurementViewModel : ViewModel() {
    private val repository = ConstructionRepository()

    private val _orders = mutableStateOf<List<MaterialOrder>>(emptyList())
    val orders: State<List<MaterialOrder>> = _orders

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    // UI State for Dialogs
    var showAddOrder by mutableStateOf(false)
        private set
    var showSendMessage by mutableStateOf(false)
        private set
    var editingOrder by mutableStateOf<MaterialOrder?>(null)
        private set
    var selectedProjectIdForNewOrder by mutableStateOf<Int?>(null)
        private set

    // Send Message Dialog State
    var drivers = mutableStateOf<List<UserProfile>>(emptyList())
        private set
    var messageContent by mutableStateOf("")
    var selectedDriverId by mutableStateOf<String?>(null)

    // Add Order Dialog State
    var materialName by mutableStateOf("")
    var quantity by mutableStateOf("")
    var unitPrice by mutableStateOf("")
    var supplier by mutableStateOf("")
    var requiredStage by mutableStateOf("Foundation")
    var projects = mutableStateOf<List<ConstructionProject>>(emptyList())
        private set

    init {
        fetchOrders()
        setupRealtime()
    }

    fun onAddOrderClick(projectId: Int? = null) {
        selectedProjectIdForNewOrder = projectId
        showAddOrder = true
        fetchProjectsForDialog()
    }

    fun onDismissAddOrder() {
        showAddOrder = false
        clearAddOrderState()
    }

    private fun clearAddOrderState() {
        materialName = ""
        quantity = ""
        unitPrice = ""
        supplier = ""
        requiredStage = "Foundation"
    }

    fun onSendMessageClick() {
        showSendMessage = true
        fetchDriversForDialog()
    }

    fun onDismissSendMessage() {
        showSendMessage = false
        messageContent = ""
        selectedDriverId = null
    }

    fun onEditOrderClick(order: MaterialOrder) {
        editingOrder = order
    }

    fun onDismissEditOrder() {
        editingOrder = null
    }

    private fun fetchProjectsForDialog() {
        viewModelScope.launch {
            projects.value = repository.getProjects()
        }
    }

    private fun fetchDriversForDialog() {
        viewModelScope.launch {
            drivers.value = repository.getAllDrivers()
        }
    }

    fun sendMessage() {
        val driverId = selectedDriverId ?: return
        val content = messageContent
        if (content.isBlank()) return

        viewModelScope.launch {
            val senderId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            repository.sendMessage(Message(senderId = senderId, receiverId = driverId, content = content))
            onDismissSendMessage()
        }
    }

    fun createOrder() {
        val project = selectedProjectIdForNewOrder?.let { id -> projects.value.find { it.id == id } } 
            ?: projects.value.firstOrNull() ?: return
        if (materialName.isBlank()) return

        viewModelScope.launch {
            try {
                repository.createMaterialOrder(
                    MaterialOrder(
                        projectId = project.id!!,
                        materialName = materialName,
                        quantity = quantity.toDoubleOrNull() ?: 0.0,
                        unit = "Units",
                        unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                        requiredStage = requiredStage,
                        supplierName = supplier,
                        status = "Pending",
                        organizationId = project.organizationId
                    )
                )
                fetchOrders()
                onDismissAddOrder()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRealtime() {
        val channel = SupabaseClient.client.channel("material_orders_sync")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "material_orders"
        }
        
        flow.onEach { 
            fetchOrders() // Simple refresh on any change
        }.launchIn(viewModelScope)
        
        viewModelScope.launch {
            channel.subscribe()
        }
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch projects first then their orders, or just fetch all material orders for user.
                val projects = repository.getProjects()
                val allOrders = mutableListOf<MaterialOrder>()
                projects.forEach { project ->
                    project.id?.let { id ->
                        allOrders.addAll(repository.getOrdersByProject(id))
                    }
                }
                _orders.value = allOrders
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateOrder(order: MaterialOrder) {
        viewModelScope.launch {
            try {
                repository.updateMaterialOrder(order)
                fetchOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteOrder(orderId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMaterialOrder(orderId)
                fetchOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
