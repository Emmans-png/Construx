package com.collins.todo.ui.screens.pages.transporter

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.repository.ConstructionRepository
import kotlinx.coroutines.launch

class TransporterViewModel : ViewModel() {
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
                _orders.value = repository.getTransporterOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
