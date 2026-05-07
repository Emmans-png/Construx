package com.collins.todo.ui.screens.pages.team

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.TeamMember
import com.collins.todo.data.repository.ConstructionRepository
import kotlinx.coroutines.launch

class TeamViewModel : ViewModel() {
    private val repository = ConstructionRepository()

    private val _teamMembers = mutableStateOf<List<TeamMember>>(emptyList())
    val teamMembers: State<List<TeamMember>> = _teamMembers

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        fetchTeam()
    }

    fun fetchTeam() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _teamMembers.value = repository.getTeam()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTeamMember(name: String, role: String, phoneNumber: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.addTeamMember(
                    TeamMember(name = name, role = role, phoneNumber = phoneNumber)
                )
                if (result != null) {
                    fetchTeam()
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
