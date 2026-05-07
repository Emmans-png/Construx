package com.collins.todo.ui.screens.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.data.Models.ConstructionProject
import com.collins.todo.data.repository.SupabaseClient
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import io.github.jan.supabase.auth.auth
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    onNavigateToProcurement: () -> Unit,
    onNavigateToROI: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToContact: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onAddProject: () -> Unit,
    onEditProject: (ConstructionProject) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by homeViewModel.projects
    val isLoading by homeViewModel.isLoading
    var showMenu by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ConstructionProject?>(null) }
    
    val currentUser = remember { SupabaseClient.client.auth.currentUserOrNull() }
    val displayUsername = remember(currentUser) { 
        currentUser?.userMetadata?.get("username")?.toString()?.removeSurrounding("\"") ?: "Developer"
    }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Site Manager: $displayUsername",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            "CONSTRUX SUITE",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ) 
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("About", color = Color.White) },
                            onClick = { showMenu = false; onNavigateToAbout() },
                            leadingIcon = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Contact", color = Color.White) },
                            onClick = { showMenu = false; onNavigateToContact() },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                        DropdownMenuItem(
                            text = { Text("Logout", color = Color.White) },
                            onClick = { showMenu = false; authViewModel.signOut(onLogout) },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProject,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        },
        containerColor = Color.Black,
        modifier = modifier
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardCard(
                            title = "Procurement",
                            value = "Stage-Gate",
                            icon = Icons.Default.Inventory,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToProcurement,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardCard(
                            title = "ROI Analyzer",
                            value = "Simulator",
                            icon = Icons.Default.Calculate,
                            color = Color(0xFFFFA000),
                            onClick = onNavigateToROI,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Active Projects", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                if (projects.isEmpty()) {
                    item { EmptyState("No construction projects tracked.") }
                } else {
                    items(projects) { project ->
                        ProjectCard(
                            project = project,
                            onEdit = { onEditProject(project) },
                            onDelete = { projectToDelete = project }
                        )
                    }
                }
            }

            if (projectToDelete != null) {
                AlertDialog(
                    onDismissRequest = { projectToDelete = null },
                    title = { Text("Delete Project") },
                    text = { Text("Are you sure you want to delete '${projectToDelete?.name}'? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                projectToDelete?.id?.let { homeViewModel.deleteProject(it) }
                                projectToDelete = null
                            }
                        ) {
                            Text("DELETE", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { projectToDelete = null }) {
                            Text("CANCEL", color = Color.White)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color.White,
                    textContentColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, value: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
        }
    }
}

@Composable
fun ProjectCard(
    project: ConstructionProject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val df = DecimalFormat("#,###.##")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(project.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        project.currentStage, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontSize = 10.sp, 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem("Budget", "$${df.format(project.totalBudget)}")
                InfoItem("Target Yield", "${df.format((project.targetRentalIncome * 12 / project.totalBudget) * 100)}%")
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
fun EmptyState(text: String) {
    Text(text, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center)
}
