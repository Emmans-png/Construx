package com.collins.todo.ui.screens.pages.procurement

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.repository.ConstructionRepository
import kotlinx.coroutines.launch

class ProcurementViewModel : ViewModel() {
    private val repository = ConstructionRepository()

    private val _orders = mutableStateOf<List<MaterialOrder>>(emptyList())
    val orders: State<List<MaterialOrder>> = _orders

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        fetchOrders()
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch projects first then their orders, or just fetch all material orders for user.
                // For simplicity, we assume we fetch orders across all projects the user owns.
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
}
