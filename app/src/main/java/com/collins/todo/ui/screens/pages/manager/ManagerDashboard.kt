package com.collins.todo.ui.screens.pages.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.data.Models.ConstructionProject
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import com.collins.todo.ui.screens.pages.home.HomeViewModel
import com.collins.todo.ui.screens.pages.procurement.AddOrderDialog
import com.collins.todo.ui.screens.pages.procurement.EditOrderDialog
import com.collins.todo.ui.screens.pages.procurement.ProcurementViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboard(
    viewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToTeam: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToFleet: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onAddProject: () -> Unit,
    onEditProject: (ConstructionProject) -> Unit
) {
    val projects by viewModel.projects
    val orders by viewModel.orders
    val drivers by viewModel.drivers
    val isLoading by viewModel.isLoading
    val unreadMessages by viewModel.unreadMessageCount
    val repository = remember { com.collins.todo.data.repository.ConstructionRepository() }
    val scope = rememberCoroutineScope()
    val procurementViewModel: ProcurementViewModel = viewModel()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MANAGER DASHBOARD", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = {
                        viewModel.clearUnreadCount()
                        onNavigateToMessages()
                    }) {
                        BadgedBox(
                            badge = {
                                if (unreadMessages > 0) {
                                    Badge { Text(unreadMessages.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, "Messages")
                        }
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, "Profile")
                    }
                    IconButton(onClick = { authViewModel.signOut(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                NavigationBarItem(
                    selected = true,
                    onClick = onNavigateToProjects,
                    icon = { Icon(Icons.Default.Architecture, null) },
                    label = { Text("Projects") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMaterials,
                    icon = { Icon(Icons.Default.Inventory, null) },
                    label = { Text("Materials") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToTeam,
                    icon = { Icon(Icons.Default.Groups, null) },
                    label = { Text("Team") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAnalytics,
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("Analytics") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProject,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                FinancialSummaryCard(projects)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Current ROI", "12.4%", Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f)) {}
                    MetricCard("Total Drivers", drivers.size.toString(), Icons.Default.LocalShipping, Modifier.weight(1f)) {
                        onNavigateToFleet()
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Projects", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onAddProject) {
                        Icon(Icons.Default.AddCircle, "Add Project", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            } else if (projects.isEmpty()) {
                item {
                    Text("No active projects found.", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        onEdit = { onEditProject(project) },
                        onDelete = {
                            scope.launch {
                                project.id?.let { viewModel.deleteProject(it) }
                                viewModel.fetchProjects()
                            }
                        },
                        onAddOrder = {
                            procurementViewModel.onAddOrderClick(project.id)
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Order Flow", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            if (isLoading) {
                // Already shown above if loading
            } else if (orders.isEmpty()) {
                item { 
                    Text("No active orders found.", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                items(orders.take(5)) { order ->
                    OrderFlowItem(
                        order = order,
                        onEdit = { procurementViewModel.onEditOrderClick(order) },
                        onDelete = {
                            scope.launch {
                                order.id?.let { repository.deleteMaterialOrder(it) }
                                viewModel.fetchOrders()
                            }
                        },
                        onTrack = {
                            order.id?.let { onNavigateToMaterials() }
                        }
                    )
                }
            }
        }

        if (procurementViewModel.showAddOrder) {
            AddOrderDialog(
                viewModel = procurementViewModel
            )
        }

        procurementViewModel.editingOrder?.let { order ->
            EditOrderDialog(
                order = order,
                onDismiss = { procurementViewModel.onDismissEditOrder() },
                onOrderUpdated = { updated ->
                    scope.launch {
                        repository.updateMaterialOrder(updated)
                        viewModel.fetchOrders()
                        procurementViewModel.onDismissEditOrder()
                    }
                }
            )
        }
    }
}

@Composable
fun FinancialSummaryCard(projects: List<ConstructionProject>) {
    val totalBudget = projects.sumOf { it.totalBudget }
    val actualSpend = totalBudget * 0.65 // Mock actual spend

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Budget vs Actual", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$${String.format(Locale.getDefault(), "%,.0f", actualSpend)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Text("of $${String.format(Locale.getDefault(), "%,.0f", totalBudget)}", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
            }
            
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.65f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
        }
    }
}

@Composable
fun ProjectCard(
    project: ConstructionProject,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddOrder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        project.name, 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddOrder, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Order", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    project.currentStage.uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem("Budget", "$${String.format(Locale.getDefault(), "%,.0f", project.totalBudget)}")
                InfoItem("Location", project.location)
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OrderFlowItem(
    order: MaterialOrder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTrack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable { onTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocalShipping, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(order.materialName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Supplier: ${order.supplierName}", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
            
            Surface(
                color = when (order.status) {
                    "Delivered" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    "Dispatched" -> Color(0xFF2196F3).copy(alpha = 0.1f)
                    "Ongoing" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> Color(0xFFFFA000).copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    order.status.uppercase(),
                    color = when (order.status) {
                        "Delivered" -> Color(0xFF4CAF50)
                        "Dispatched" -> Color(0xFF2196F3)
                        "Ongoing" -> MaterialTheme.colorScheme.primary
                        else -> Color(0xFFFFA000)
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
