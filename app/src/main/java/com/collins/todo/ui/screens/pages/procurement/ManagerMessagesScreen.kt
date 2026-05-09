package com.collins.todo.ui.screens.pages.procurement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.collins.todo.data.Models.Message
import com.collins.todo.data.Models.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerMessagesScreen(onBack: () -> Unit) {
    val viewModel: ManagerMessagesViewModel = viewModel()
    val drivers by viewModel.drivers
    val selectedDriver by viewModel.selectedDriver
    val messages by viewModel.messages
    val isLoading by viewModel.isLoading

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = selectedDriver?.username?.uppercase() ?: "DRIVER CHATS",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedDriver != null) {
                            viewModel.selectDriver(null)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedDriver == null) {
                // Driver List View
                DriverListView(
                    drivers = drivers,
                    onDriverClick = { viewModel.selectDriver(it) },
                    isLoading = isLoading
                )
            } else {
                // Chat View
                ChatView(
                    messages = messages,
                    currentUserId = viewModel.currentUserId,
                    onSendMessage = { viewModel.sendMessage(it) }
                )
            }
        }
    }
}

@Composable
fun DriverListView(
    drivers: List<UserProfile>,
    onDriverClick: (UserProfile) -> Unit,
    isLoading: Boolean
) {
    if (isLoading && drivers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (drivers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Chat, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("No drivers found in your fleet.", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(drivers) { driver ->
                DriverChatCard(driver = driver, onClick = { onDriverClick(driver) })
            }
        }
    }
}

@Composable
fun DriverChatCard(driver: UserProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(driver.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Transporter - ${driver.vehiclePlate ?: "No Vehicle"}", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Chat, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ChatView(
    messages: List<Message>,
    currentUserId: String?,
    onSendMessage: (String) -> Unit
) {
    var textState by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(messages.sortedByDescending { it.createdAt }) { message ->
                val isMe = message.senderId == currentUserId
                ChatBubble(message = message, isMe = isMe)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp).navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Type a message...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                IconButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            onSendMessage(textState)
                            textState = ""
                        }
                    },
                    enabled = textState.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primary else Color.DarkGray,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 14.sp
            )
        }
        Text(
            text = message.createdAt?.take(16)?.replace("T", " ") ?: "Just now",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
