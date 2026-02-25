package com.friction.app.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.friction.app.ui.theme.FrictionColors
import com.friction.app.ui.theme.FrictionTheme
import com.friction.app.utils.PermissionUtils

@Composable
fun PermissionsScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var isUsageStatsEnabled by remember { mutableStateOf(PermissionUtils.isUsageStatsEnabled(context)) }

    // Re-check permissions when returning to the app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)
                isUsageStatsEnabled = PermissionUtils.isUsageStatsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    FrictionTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FrictionColors.Background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "friction",
                fontFamily = FontFamily.Monospace,
                fontSize = 32.sp,
                color = FrictionColors.Accent,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "To protect your time, we need a couple of permissions.",
                fontSize = 16.sp,
                color = FrictionColors.OnBackground,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            PermissionSection(
                title = "Accessibility Service",
                description = "Required to detect when you open certain apps and show the friction wall.",
                isEnabled = isAccessibilityEnabled,
                onEnable = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionSection(
                title = "Usage Stats Access",
                description = "Used to track how many times you open apps and provide insights into your habits.",
                isEnabled = isUsageStatsEnabled,
                onEnable = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onPermissionsGranted,
                enabled = isAccessibilityEnabled && isUsageStatsEnabled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FrictionColors.Accent,
                    disabledContainerColor = FrictionColors.Surface2
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "CONTINUE",
                    color = if (isAccessibilityEnabled && isUsageStatsEnabled) Color.Black else FrictionColors.Muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun PermissionSection(
    title: String,
    description: String,
    isEnabled: Boolean,
    onEnable: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FrictionColors.Surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FrictionColors.OnBackground,
                    modifier = Modifier.weight(1f)
                )
                if (isEnabled) {
                    Text("✅", fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                fontSize = 12.sp,
                color = FrictionColors.Muted,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isEnabled) {
                Button(
                    onClick = onEnable,
                    colors = ButtonDefaults.buttonColors(containerColor = FrictionColors.Accent.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Enable", color = FrictionColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Granted",
                    color = FrictionColors.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}


