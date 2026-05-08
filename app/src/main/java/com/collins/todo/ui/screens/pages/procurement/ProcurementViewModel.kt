package com.collins.todo.ui.screens.pages.procurement

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.repository.ConstructionRepository
import com.collins.todo.data.repository.SupabaseClient
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

    init {
        fetchOrders()
        setupRealtime()
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
