package com.collins.todo.ui.screens.pages.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.ConstructionProject
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.repository.ConstructionRepository
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

    init {
        refreshAll()
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
                profile?.organizationId?.let { orgId ->
                    _drivers.value = repository.getDriversByOrganization(orgId)
                }
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
