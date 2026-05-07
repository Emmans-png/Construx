package com.collins.todo.ui.screens.pages.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import com.collins.todo.ui.screens.pages.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboard(
    viewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToTeam: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val projects by viewModel.projects
    val isLoading by viewModel.isLoading

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MANAGER DASHBOARD", fontWeight = FontWeight.Black) },
                actions = {
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
        containerColor = MaterialTheme.colorScheme.background
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
                    MetricCard("Current ROI", "12.4%", Icons.Default.TrendingUp, Modifier.weight(1f))
                    MetricCard("Avg Completion", "78 days", Icons.Default.Event, Modifier.weight(1f))
                }
            }

            item {
                Text("Live Order Flow", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            } else {
                items(projects.take(5)) { project ->
                    OrderFlowItem(project)
                }
            }
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
                Text("$${String.format("%,.0f", actualSpend)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Text("of $${String.format("%,.0f", totalBudget)}", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
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
fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
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
fun OrderFlowItem(project: ConstructionProject) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LocalShipping, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(project.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("In Transit • Expected today", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
        }
        Surface(
            color = Color(0xFFFFA000).copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("IN TRANSIT", color = Color(0xFFFFA000), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}
