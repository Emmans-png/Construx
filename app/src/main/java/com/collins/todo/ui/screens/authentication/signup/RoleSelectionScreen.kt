package com.collins.todo.ui.screens.authentication.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.collins.todo.ui.screens.authentication.login.AuthViewModel

@Composable
fun RoleSelectionScreen(
    viewModel: AuthViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        AsyncImage(
            model = "https://images.unsplash.com/photo-153183436ea12-3269668d2bc0?q=80&w=1974&auto=format&fit=crop",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CONSTRUX",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "IDENTITY SELECTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 6.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Choose Your Path",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Select your primary role in the construction supply chain ecosystem.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            RoleCard(
                title = "Company Manager",
                description = "Plan investments, track ROI, and approve project budgets.",
                icon = Icons.Default.BusinessCenter,
                isSelected = viewModel.role == "Manager",
                onClick = { viewModel.role = "Manager" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Transporter / Driver",
                description = "Manage deliveries, use live GPS tracking, and fulfill orders.",
                icon = Icons.Default.LocalShipping,
                isSelected = viewModel.role == "Transporter",
                onClick = { viewModel.role = "Transporter" }
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (viewModel.role.isNotBlank()) {
                        onNext()
                    } else {
                        viewModel.errorMessage = "Please select your role to continue"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("CONTINUE", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            TextButton(onClick = onBack) {
                Text("Back to Sign In", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
