package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4

    // Check actual permissions in real-time
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    // Check permission helper from OverlayCoordinator
    val isAccessibilityGranted by com.example.service.OverlayCoordinator.isAccessibilityActive.collectAsState()

    DisposableEffect(Unit) {
        val observer = {
            isOverlayGranted = Settings.canDrawOverlays(context)
        }
        // Easy polling check or regular check is done on resumption, but in onboarding we can check when user re-enters.
        onDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A0A), Color(0xFF151515))
                )
            )
            .padding(24.dp)
    ) {
        // Top row for skip button
        if (currentStep < totalSteps) {
            TextButton(
                onClick = { onOnboardingFinished() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp)
                    .testTag("skip_onboarding_button")
            ) {
                Text(
                    text = "SKIP",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Main content card with entry logic animations
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() with
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() with
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "OnboardingContent"
            ) { step ->
                when (step) {
                    1 -> WelcomeStep()
                    2 -> OverlayPermissionStep(isOverlayGranted) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not launch overlays settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                    3 -> AccessibilityPermissionStep(isAccessibilityGranted) {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                            Toast.makeText(context, "Locate 'Arc Clone' in installed apps and turn it ON.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open accessibility settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                    4 -> CompletedStep()
                }
            }
        }

        // Bottom Navigation Block (Dots + Buttons)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dot Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..totalSteps) {
                    val isActive = i == currentStep
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 18.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (isActive) Color(0xFF3B82F6) else Color(0xFF2C2C2E))
                    )
                }
            }

            // Navigation Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("onboarding_back_button")
                    ) {
                        Text("BACK", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            onOnboardingFinished()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (currentStep == totalSteps) "GET STARTED" else "CONTINUE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_step_1")
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
                .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Arc Logo",
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Welcome to Arc Clone",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your proactive, premium on-device companion. Arc sits gracefully over any screen to help you summarize context, check facts, edit drafts, and read screen structures using advanced LLM intelligence.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun OverlayPermissionStep(
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_step_2")
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF3B82F6).copy(alpha = 0.12f)
                )
                .border(
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGranted) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF3B82F6).copy(alpha = 0.3f)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Layers,
                contentDescription = "Overlay Permission icon",
                tint = if (isGranted) Color(0xFF10B981) else Color(0xFF60A5FA),
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "Display Over Other Apps",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Arc triggers workflows using a flexible round button overlays system. This allows you to launch summaries easily, without leaving whatever application you are currently using.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HOW TO AUTHORIZE OVERLAY:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "1. Click Authorize Overlay below.\n2. Locate 'Arc Clone' in list.\n3. Turn on permission toggle.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White
                )
            }
        }

        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF10B981) else Color(0xFF1C1C1E)
            ),
            border = if (isGranted) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("grant_overlay_button")
        ) {
            Text(
                text = if (isGranted) "OVERLAY GRANTED" else "AUTHORIZE OVERLAY",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AccessibilityPermissionStep(
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_step_3")
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF3B82F6).copy(alpha = 0.12f)
                )
                .border(
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGranted) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF3B82F6).copy(alpha = 0.3f)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Visibility,
                contentDescription = "Accessibility icon",
                tint = if (isGranted) Color(0xFF10B981) else Color(0xFF60A5FA),
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "Active Screen Reading",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "To analyze what's on your screen, our Accessibility Service reads existing layout text on-demand. High-security data is locked and never stored or cached anywhere.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HOW TO ENABLE ACCESSIBILITY:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "1. Click Enable Access below.\n2. Tap 'Arc Clone' in installed services.\n3. Active the toggle.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White
                )
            }
        }

        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF10B981) else Color(0xFF1C1C1E)
            ),
            border = if (isGranted) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("grant_accessibility_button")
        ) {
            Text(
                text = if (isGranted) "ACCESS GRANTED" else "ENABLE ACCESSIBILITY",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CompletedStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_step_4")
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Beenhere,
                contentDescription = "Success",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "You are Ready!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Welcome to the future of fluid, overlay-based machine reasoning. Tap 'Launch Overlay' from the dashboard to initialize the glowing controller, and interact with text in any application.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
