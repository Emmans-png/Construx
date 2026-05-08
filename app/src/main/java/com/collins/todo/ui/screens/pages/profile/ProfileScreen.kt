package com.collins.todo.ui.screens.pages.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.data.Models.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val profile by viewModel.profile
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    
    var isEditing by remember { mutableStateOf(false) }
    
    // Form states
    var username by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var organizationName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
    }

    LaunchedEffect(profile) {
        profile?.let {
            username = it.username
            phoneNumber = it.phoneNumber ?: ""
            organizationName = it.organizationName ?: ""
            location = it.location ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    } else {
                        IconButton(onClick = {
                            profile?.let {
                                val updated = it.copy(
                                    username = username,
                                    phoneNumber = phoneNumber,
                                    organizationName = organizationName,
                                    location = location
                                )
                                viewModel.updateProfile(updated) {
                                    isEditing = false
                                }
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }

                ProfileField("Username", username, isEditing) { username = it }
                ProfileField("Email (Read-only)", profile?.email ?: "", false) { }
                ProfileField("Phone Number", phoneNumber, isEditing) { phoneNumber = it }
                ProfileField("Role (Read-only)", profile?.role ?: "", false) { }

                if (profile?.role == "Manager") {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Organization Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ProfileField("Organization Name", organizationName, isEditing) { organizationName = it }
                    ProfileField("Location", location, isEditing) { location = it }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("DELETE ACCOUNT PERMANENTLY")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This action is permanent and cannot be undone. All your data will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(onDeleteAccount)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun ProfileField(label: String, value: String, isEditing: Boolean, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        if (isEditing) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        } else {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        }
    }
}
