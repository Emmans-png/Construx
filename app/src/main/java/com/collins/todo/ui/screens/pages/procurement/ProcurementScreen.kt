package com.collins.todo.ui.screens.pages.procurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.collins.todo.data.Models.MaterialOrder
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcurementScreen(
    onBack: () -> Unit,
    onNavigateToLiveTracking: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ProcurementViewModel = viewModel()
    val orders by viewModel.orders
    val isLoading by viewModel.isLoading

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PROCUREMENT GATE",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onSendMessageClick() }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, "Message Driver", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddOrderClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Order")
            }
        },
        containerColor = Color.Black
    ) { padding ->
        if (viewModel.showAddOrder) {
            AddOrderDialog(
                viewModel = viewModel
            )
        }

        if (viewModel.showSendMessage) {
            SendMessageDialog(viewModel = viewModel)
        }

        viewModel.editingOrder?.let { order ->
            EditOrderDialog(
                order = order,
                onDismiss = { viewModel.onDismissEditOrder() },
                onOrderUpdated = { updatedOrder ->
                    viewModel.updateOrder(updatedOrder)
                    viewModel.onDismissEditOrder()
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Stage-Gate Procurement",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Approve orders based on construction stage gates",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active material orders.", color = MaterialTheme.colorScheme.tertiary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val grouped = orders.groupBy { it.requiredStage }
                    grouped.forEach { (stage, stageOrders) ->
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    stage.uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(stageOrders) { order ->
                            OrderCard(
                                order = order,
                                onTrack = { order.id?.let { onNavigateToLiveTracking(it) } },
                                onEdit = { viewModel.onEditOrderClick(order) },
                                onDelete = { order.id?.let { viewModel.deleteOrder(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SendMessageDialog(viewModel: ProcurementViewModel) {
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { viewModel.onDismissSendMessage() },
        title = { Text("Send Message to Driver", color = Color.White) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        val drivers = viewModel.drivers.value
                        Text(drivers.find { it.id == viewModel.selectedDriverId }?.username ?: "Select Driver")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        viewModel.drivers.value.forEach { driver ->
                            DropdownMenuItem(
                                text = { Text(driver.username) },
                                onClick = {
                                    viewModel.selectedDriverId = driver.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                TextField(
                    value = viewModel.messageContent,
                    onValueChange = { viewModel.messageContent = it },
                    label = { Text("Message Content") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                enabled = viewModel.selectedDriverId != null && viewModel.messageContent.isNotBlank(),
                onClick = { viewModel.sendMessage() }
            ) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = { viewModel.onDismissSendMessage() }) { Text("Cancel") } }
    )
}

@Composable
fun EditOrderDialog(
    order: MaterialOrder,
    onDismiss: () -> Unit,
    onOrderUpdated: (MaterialOrder) -> Unit
) {
    var materialName by remember { mutableStateOf(order.materialName) }
    var quantity by remember { mutableStateOf(order.quantity.toString()) }
    var unitPrice by remember { mutableStateOf(order.unitPrice.toString()) }
    var supplier by remember { mutableStateOf(order.supplierName) }
    var status by remember { mutableStateOf(order.status) }
    
    val statuses = listOf("Pending", "Dispatched", "Ongoing", "Delivered")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Material Order", color = Color.White) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = materialName, onValueChange = { materialName = it }, label = { Text("Material Name") })
                TextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") })
                TextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Unit Price ($)") })
                TextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier") })
                
                Text("Status", color = Color.White, fontSize = 12.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onOrderUpdated(
                    order.copy(
                        materialName = materialName,
                        quantity = quantity.toDoubleOrNull() ?: order.quantity,
                        unitPrice = unitPrice.toDoubleOrNull() ?: order.unitPrice,
                        supplierName = supplier,
                        status = status
                    )
                )
            }) { Text("Update Order") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddOrderDialog(
    viewModel: ProcurementViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { viewModel.onDismissAddOrder() },
        title = { Text("New Material Order", color = Color.White) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (viewModel.selectedProjectIdForNewOrder == null && viewModel.projects.value.isNotEmpty()) {
                    Text("Select Project", color = Color.White, fontSize = 12.sp)
                    var expandedProject by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedProject = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(viewModel.projects.value.find { it.id == viewModel.selectedProjectIdForNewOrder }?.name ?: "Select Project")
                        }
                        DropdownMenu(expanded = expandedProject, onDismissRequest = { expandedProject = false }) {
                            viewModel.projects.value.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        viewModel.onAddOrderClick(p.id)
                                        expandedProject = false
                                    }
                                )
                            }
                        }
                    }
                }

                TextField(value = viewModel.materialName, onValueChange = { viewModel.materialName = it }, label = { Text("Material Name") })
                TextField(value = viewModel.quantity, onValueChange = { viewModel.quantity = it }, label = { Text("Quantity") })
                TextField(value = viewModel.unitPrice, onValueChange = { viewModel.unitPrice = it }, label = { Text("Unit Price ($)") })
                TextField(value = viewModel.supplier, onValueChange = { viewModel.supplier = it }, label = { Text("Supplier") })
                
                Text("Required Stage", color = Color.White, fontSize = 12.sp)
                val stages = listOf("Foundation", "Walling", "Roofing", "Finishing")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3
                ) {
                    stages.forEach { s ->
                        FilterChip(
                            selected = viewModel.requiredStage == s,
                            onClick = { viewModel.requiredStage = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                viewModel.statusMessage?.let {
                    Text(
                        it, 
                        color = if (it.startsWith("Error") || it.startsWith("Failed")) Color.Red else Color.Green,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    enabled = !viewModel.isSaving && (viewModel.selectedProjectIdForNewOrder != null || viewModel.projects.value.isNotEmpty()) && viewModel.materialName.isNotBlank(),
                    onClick = { viewModel.createOrder() }
                ) { 
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Add Order") 
                }
            }
        },
        dismissButton = { 
            TextButton(
                enabled = !viewModel.isSaving,
                onClick = { viewModel.onDismissAddOrder() }
            ) { Text("Cancel") } 
        }
    )
}

@Composable
fun OrderCard(
    order: MaterialOrder,
    onTrack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Inventory, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.materialName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Supplier: ${order.supplierName}", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                    Text("${order.quantity} ${order.unit} @ $${order.unitPrice}", color = Color.White, fontSize = 14.sp)
                    if (order.estimatedDays != null) {
                        Text("ETA: ${order.estimatedDays} days", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }

                Surface(
                    color = when (order.status) {
                        "Delivered" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "Dispatched" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        "Ongoing" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else -> Color(0xFFFFA000).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        order.status,
                        color = when (order.status) {
                            "Delivered" -> Color(0xFF4CAF50)
                            "Dispatched" -> Color(0xFF2196F3)
                            "Ongoing" -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFFFA000)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (order.status == "Ongoing" || order.status == "Dispatched") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onTrack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("VIEW LIVE MOVEMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
