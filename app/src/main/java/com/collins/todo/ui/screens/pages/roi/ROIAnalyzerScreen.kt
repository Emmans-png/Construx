package com.collins.todo.ui.screens.pages.roi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ROIAnalyzerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var landCost by remember { mutableStateOf("50000") }
    var constructionCost by remember { mutableStateOf("150000") }
    var monthlyRental by remember { mutableStateOf("2500") }

    val totalInvestment = (landCost.toDoubleOrNull() ?: 0.0) + (constructionCost.toDoubleOrNull() ?: 0.0)
    val annualIncome = (monthlyRental.toDoubleOrNull() ?: 0.0) * 12
    val yield = if (totalInvestment > 0) (annualIncome / totalInvestment) * 100 else 0.0
    val breakEvenYears = if (annualIncome > 0) totalInvestment / annualIncome else 0.0

    val df = DecimalFormat("#.##")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ROI SIMULATOR",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Investment Parameters",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            InputLabel("Land Cost ($)")
            SimpleTextField(landCost) { landCost = it }

            Spacer(modifier = Modifier.height(16.dp))

            InputLabel("Construction Cost ($)")
            SimpleTextField(constructionCost) { constructionCost = it }

            Spacer(modifier = Modifier.height(16.dp))

            InputLabel("Expected Monthly Rental ($)")
            SimpleTextField(monthlyRental) { monthlyRental = it }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Simulation Results",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ResultCard("Annual Yield", "${df.format(yield)}%", Modifier.weight(1f))
                ResultCard("Break Even", "${df.format(breakEvenYears)} Yrs", Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ResultCard("Total Capital Required", "$${df.format(totalInvestment)}", Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun InputLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun SimpleTextField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(4.dp)
    )
}

@Composable
fun ResultCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
            Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}
