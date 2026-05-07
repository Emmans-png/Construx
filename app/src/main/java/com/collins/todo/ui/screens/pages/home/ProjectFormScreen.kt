package com.collins.todo.ui.screens.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.ConstructionProject
import com.collins.todo.data.repository.ConstructionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormScreen(
    projectId: Int? = null,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var landCost by remember { mutableStateOf("") }
    var rentalIncome by remember { mutableStateOf("") }
    var currentStage by remember { mutableStateOf("Foundation") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val repository = ConstructionRepository()
    val scope = rememberCoroutineScope()

    LaunchedEffect(projectId) {
        if (projectId != null) {
            isLoading = true
            try {
                val project = repository.getProjectById(projectId)
                if (project != null) {
                    name = project.name
                    location = project.location
                    budget = project.totalBudget.toString()
                    landCost = project.landCost.toString()
                    rentalIncome = project.targetRentalIncome.toString()
                    currentStage = project.currentStage
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load project details."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (projectId == null) "NEW PROJECT" else "EDIT PROJECT", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProjectInputField("Project Name", name) { name = it }
            ProjectInputField("Location", location) { location = it }
            ProjectInputField("Total Budget ($)", budget) { budget = it }
            ProjectInputField("Land Cost ($)", landCost) { landCost = it }
            ProjectInputField("Target Monthly Rental ($)", rentalIncome) { rentalIncome = it }
            
            if (projectId != null) {
                Text("Current Stage", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                val stages = listOf("Foundation", "Walling", "Roofing", "Finishing")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stages.forEach { stage ->
                        val isSelected = currentStage == stage
                        FilterChip(
                            selected = isSelected,
                            onClick = { currentStage = stage },
                            label = { Text(stage, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val project = ConstructionProject(
                                id = projectId,
                                name = name,
                                location = location,
                                totalBudget = budget.toDoubleOrNull() ?: 0.0,
                                landCost = landCost.toDoubleOrNull() ?: 0.0,
                                targetRentalIncome = rentalIncome.toDoubleOrNull() ?: 0.0,
                                currentStage = currentStage
                            )
                            
                            val result = if (projectId == null) {
                                repository.createProject(project)
                            } else {
                                repository.updateProject(project)
                            }

                            if (result != null) {
                                onSaveSuccess()
                            } else {
                                errorMessage = "Failed to save project. Please try again."
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorMessage = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(4.dp),
                enabled = !isLoading && name.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(if (projectId == null) "CREATE PROJECT" else "UPDATE PROJECT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProjectInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(4.dp)
        )
    }
}
