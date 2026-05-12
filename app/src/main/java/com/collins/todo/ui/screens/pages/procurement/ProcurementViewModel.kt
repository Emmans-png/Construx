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

    private val _unreadMessageCount = mutableStateOf(0)
    val unreadMessageCount: State<Int> = _unreadMessageCount

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
    var earnings by mutableStateOf("")
    var supplier by mutableStateOf("")
    var requiredStage by mutableStateOf("Foundation")
    var projects = mutableStateOf<List<ConstructionProject>>(emptyList())
        private set
    
    var statusMessage by mutableStateOf<String?>(null)
    var isSaving by mutableStateOf(false)

    init {
        fetchOrders()
        fetchUnreadCount()
        setupRealtime()
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull() ?: return@launch
                val messages = repository.getMessagesForUser()
                _unreadMessageCount.value = messages.count { it.receiverId == user.id && !it.isRead }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearUnreadCount() {
        _unreadMessageCount.value = 0
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
        earnings = ""
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
        val project = if (selectedProjectIdForNewOrder != null) {
            projects.value.find { it.id == selectedProjectIdForNewOrder }
        } else {
            projects.value.firstOrNull()
        }

        if (project == null) {
            statusMessage = "Error: No project selected"
            return
        }
        
        if (materialName.isBlank()) {
            statusMessage = "Error: Material name is required"
            return
        }

        viewModelScope.launch {
            isSaving = true
            statusMessage = "Saving order..."
            try {
                // Ensure organizationId is a valid UUID string or null (prevent empty string errors)
                val orgId = if (project.organizationId.isNullOrBlank()) null else project.organizationId

                val newOrder = MaterialOrder(
                    projectId = project.id!!,
                    materialName = materialName,
                    quantity = quantity.toDoubleOrNull() ?: 0.0,
                    unit = "Units",
                    unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                    earnings = earnings.toDoubleOrNull() ?: 0.0,
                    requiredStage = requiredStage,
                    supplierName = supplier,
                    status = "Pending",
                    organizationId = orgId
                )
                
                println("VM_ACTION: Inserting order with OrgID: $orgId")
                val result = repository.createMaterialOrder(newOrder)
                
                if (result != null) {
                    statusMessage = "Order created successfully!"
                    fetchOrders()
                    kotlinx.coroutines.delay(1000)
                    onDismissAddOrder()
                } else {
                    statusMessage = "Failed to save order. Check database permissions."
                }
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
                e.printStackTrace()
            } finally {
                isSaving = false
            }
        }
    }

    private fun setupRealtime() {
        val channel = SupabaseClient.client.channel("procurement_sync")
        
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "material_orders"
        }.onEach { 
            fetchOrders() 
        }.launchIn(viewModelScope)

        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }.onEach {
            fetchUnreadCount()
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
