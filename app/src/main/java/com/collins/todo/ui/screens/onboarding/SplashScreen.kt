package com.collins.todo.ui.screens.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.collins.todo.data.repository.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: (Boolean, Boolean) -> Unit // Boolean: isLoggedIn, isFirstLaunch
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        delay(2000) // Show for 2 seconds
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val sharedPref = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPref.getBoolean("isFirstLaunch", true)
        onFinished(session != null, isFirstLaunch)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Background Image restored
        AsyncImage(
            model = "https://static.vecteezy.com/system/resources/thumbnails/022/453/397/small_2x/silhouette-of-engineer-looking-at-construction-site-engineering-concept-double-exposure-generative-ai-free-photo.jpg",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f // Dimmed for text readability
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CONSTRUX",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp
            )
            Text(
                text = "SHELTERFLOW SUITE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 8.sp
            )
        }
    }
}
