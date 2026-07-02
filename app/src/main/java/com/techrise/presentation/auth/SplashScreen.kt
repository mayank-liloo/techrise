package com.techrise.presentation.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techrise.R
import com.techrise.data.repository.TechRiseRepository
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    repository: TechRiseRepository,
    onNavigateToAuth: () -> Unit,
    onNavigateToCustomerHome: () -> Unit,
    onNavigateToAdminHome: () -> Unit
) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Run scale and fade in animation in parallel
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        
        // Wait for splash delay (2.5 seconds total)
        delay(1500)

        // Navigate based on auth status
        if (repository.isLoggedIn()) {
            val role = repository.getRole() ?: "CUSTOMER"
            if (role.uppercase() == "ADMIN") {
                onNavigateToAdminHome()
            } else {
                onNavigateToCustomerHome()
            }
        } else {
            onNavigateToAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF3E0), // Soft warm peach/orange background
                        Color(0xFFFFE0B2)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Tech Rise Logo",
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
            )


        }
    }
}
