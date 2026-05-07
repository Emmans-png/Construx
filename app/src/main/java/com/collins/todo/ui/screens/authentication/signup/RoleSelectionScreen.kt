package com.collins.todo.ui.screens.authentication.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.ui.screens.authentication.login.AuthViewModel

@Composable
fun RoleSelectionScreen(
    viewModel: AuthViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CONSTRUX",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-2).sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "Supply Chain Role",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )

            Text(
                text = "What is your primary role in the supply chain?",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start).padding(top = 16.dp, bottom = 32.dp)
            )

            RoleCard(
                title = "Company Manager",
                description = "Focus: Investment planning, ROI, budget approval.",
                isSelected = viewModel.role == "Manager",
                onClick = { viewModel.role = "Manager" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Transporter/Driver",
                description = "Focus: Material delivery, GPS tracking, order fulfillment.",
                isSelected = viewModel.role == "Transporter",
                onClick = { viewModel.role = "Transporter" }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (viewModel.role.isNotBlank()) {
                        onNext()
                    } else {
                        viewModel.errorMessage = "Please select a role"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onBack) {
                Text("Back", color = Color.White)
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
