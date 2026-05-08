package com.collins.todo.ui.screens.pages.transporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.data.Models.Message
import com.collins.todo.data.repository.ConstructionRepository
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMessagesScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repository = ConstructionRepository()
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var replyText by remember { mutableStateOf("") }
    var selectedMessageForReply by remember { mutableStateOf<Message?>(null) }

    fun refreshMessages() {
        scope.launch {
            isLoading = true
            messages = repository.getMessagesForUser()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshMessages()
    }

    if (selectedMessageForReply != null) {
        AlertDialog(
            onDismissRequest = { selectedMessageForReply = null; replyText = "" },
            title = { Text("Reply to Manager", color = Color.White) },
            containerColor = Color.DarkGray,
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(
                    enabled = replyText.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val user = SupabaseClient.client.auth.currentUserOrNull()
                            if (user != null && selectedMessageForReply != null) {
                                val success = repository.sendMessage(
                                    Message(
                                        senderId = user.id,
                                        receiverId = selectedMessageForReply!!.senderId,
                                        content = replyText
                                    )
                                )
                                if (success) {
                                    selectedMessageForReply = null
                                    replyText = ""
                                    refreshMessages()
                                }
                            }
                        }
                    }
                ) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { selectedMessageForReply = null; replyText = "" }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MESSAGES", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Chat, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No messages from management yet.", color = MaterialTheme.colorScheme.tertiary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages.sortedByDescending { it.createdAt }) { message ->
                    MessageCard(
                        message = message,
                        onReply = { selectedMessageForReply = message }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageCard(message: Message, onReply: () -> Unit) {
    val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
    val isFromMe = message.senderId == currentUserId

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFromMe) Color(0xFF1B5E20) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isFromMe) Icons.Default.Reply else Icons.Default.Message, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFromMe) "ME" else "FROM MANAGER", 
                    color = MaterialTheme.colorScheme.primary, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (!message.isRead && !isFromMe) {
                    Box(Modifier.size(8.dp).background(Color.Red, CircleShape))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(message.content, color = Color.White, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(message.createdAt?.take(16)?.replace("T", " ") ?: "Just now", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
                
                if (!isFromMe) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onReply, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Reply, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reply", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
