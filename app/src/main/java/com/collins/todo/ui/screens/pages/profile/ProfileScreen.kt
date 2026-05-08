package com.collins.todo.ui.screens.pages.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.data.Models.UserProfile
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.profile
    val stats by viewModel.stats
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        viewModel.selectedImageUri = it
    }

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
    }

    LaunchedEffect(profile) {
        profile?.let {
            viewModel.username = it.username
            viewModel.phoneNumber = it.phoneNumber ?: ""
            viewModel.organizationName = it.organizationName ?: ""
            viewModel.location = it.location ?: ""
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MY PROFILE", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (!viewModel.isEditing) {
                        IconButton(onClick = { viewModel.isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = {
                            profile?.let {
                                val updated = it.copy(
                                    username = viewModel.username,
                                    phoneNumber = viewModel.phoneNumber,
                                    organizationName = viewModel.organizationName,
                                    location = viewModel.location,
                                    profilePictureUrl = profile?.profilePictureUrl
                                )
                                viewModel.updateProfile(updated) {
                                    viewModel.isEditing = false
                                    viewModel.selectedImageUri = null // Clear selected image after update
                                }

                                viewModel.selectedImageUri?.let { uri ->
                                    viewModel.uploadProfilePicture(context, uri) { success ->
                                        if (success) {
                                            // Optionally refresh profile to show new picture
                                            viewModel.fetchProfile()
                                        }
                                        viewModel.selectedImageUri = null
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (isLoading && profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header / Avatar Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), Color.Black)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ProfilePicture(
                            imageUrl = profile?.profilePictureUrl,
                            initial = viewModel.username.take(1).uppercase(),
                            isEditing = viewModel.isEditing
                        ) {
                            imagePickerLauncher.launch("image/*")
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = profile?.role?.uppercase() ?: "USER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }

                // Stats Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    stats.forEach { (label, value) ->
                        ProfileStatCard(label, value, Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Details Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Personal Information", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        
                        ProfileInfoItem("Full Name", viewModel.username, Icons.Default.Person, viewModel.isEditing) { viewModel.username = it }
                        ProfileInfoItem("Email Address", profile?.email ?: "N/A", Icons.Default.Email, false) { }
                        ProfileInfoItem("Phone Number", viewModel.phoneNumber, Icons.Default.Phone, viewModel.isEditing) { viewModel.phoneNumber = it }
                        
                        if (profile?.role == "Manager") {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Text("Organization", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                            ProfileInfoItem("Company", viewModel.organizationName, Icons.Default.Business, viewModel.isEditing) { viewModel.organizationName = it }
                            ProfileInfoItem("Location", viewModel.location, Icons.Default.LocationOn, viewModel.isEditing) { viewModel.location = it }
                        }
                    }
                }

                // Experience / Time Info
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            val joinedDate = remember(profile?.createdAt) {
                                try {
                                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                    val date = isoFormat.parse(profile?.createdAt ?: "")
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date!!)
                                } catch (e: Exception) {
                                    "Joined Construx Today"
                                }
                            }
                            Text("Member Since", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                            Text(joinedDate, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Danger Zone
                Spacer(Modifier.height(32.dp))
                TextButton(
                    onClick = { viewModel.showDeleteDialog = true },
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("DELETE ACCOUNT PERMANENTLY", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (viewModel.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This action is permanent and cannot be undone. All your project data and history will be wiped from Construx servers.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(onDeleteAccount)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog = false }) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = Color.White,
            textContentColor = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun ProfilePicture(
    imageUrl: String?,
    initial: String,
    isEditing: Boolean,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null && imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = initial,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onImageClick) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileInfoItem(
    label: String, 
    value: String, 
    icon: ImageVector,
    isEditing: Boolean, 
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            if (isEditing) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                )
            } else {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
