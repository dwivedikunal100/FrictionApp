package com.friction.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.friction.app.data.model.FrictionMode
import com.friction.app.data.model.ProtectedApp
import com.friction.app.data.repository.AppRepository
import com.friction.app.ui.theme.FrictionColors
import com.friction.app.ui.theme.FrictionTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// ─── ViewModel ───────────────────────────────────────────────────────────────

class HomeViewModel(private val repository: AppRepository) : ViewModel() {

    val protectedApps: StateFlow<List<ProtectedApp>> = repository.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var timeSavedToday by mutableLongStateOf(0L)
        private set

    init {
        viewModelScope.launch {
            timeSavedToday = repository.getTimeSavedToday()
        }
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleApp(packageName, enabled)
        }
    }

    fun removeApp(app: ProtectedApp) {
        viewModelScope.launch {
            repository.removeApp(app)
        }
    }
}

// ─── Home Screen ─────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onNavigateToAddApp: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: HomeViewModel
) {
    val apps by viewModel.protectedApps.collectAsState()
    val context = LocalContext.current

    FrictionTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FrictionColors.Background)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "friction",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp,
                    color = FrictionColors.Accent,
                    letterSpacing = (-1).sp
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(FrictionColors.Surface, CircleShape)
                        .border(1.dp, FrictionColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Time saved card
            TimeSavedCard(
                timeSavedMs = viewModel.timeSavedToday,
                appCount = apps.count { it.isEnabled }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Accessibility service warning (if not enabled)
            if (!isAccessibilityServiceEnabled(context)) {
                AccessibilityWarningCard {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Protected apps header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROTECTED APPS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    color = FrictionColors.Muted
                )
                IconButton(
                    onClick = onNavigateToAddApp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, "Add app", tint = FrictionColors.Accent)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (apps.isEmpty()) {
                EmptyState(onAdd = onNavigateToAddApp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            onToggle = { viewModel.toggleApp(app.packageName, it) },
                            onUpgrade = onNavigateToPaywall,
                            onRemove = { viewModel.removeApp(app) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Components ──────────────────────────────────────────────────────────────

@Composable
fun TimeSavedCard(timeSavedMs: Long, appCount: Int) {
    val hours = TimeUnit.MILLISECONDS.toMinutes(timeSavedMs) / 60f

    Card(
        colors = CardDefaults.cardColors(containerColor = FrictionColors.Surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TIME SAVED TODAY",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 3.sp,
                color = FrictionColors.Muted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.1f".format(hours),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 48.sp,
                    color = FrictionColors.Accent,
                    lineHeight = 48.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "hrs",
                    fontSize = 16.sp,
                    color = FrictionColors.Muted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = "$appCount apps protected · keep going",
                fontSize = 12.sp,
                color = FrictionColors.Muted
            )
        }
    }
}

@Composable
fun AppRow(
    app: ProtectedApp,
    onToggle: (Boolean) -> Unit,
    onUpgrade: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FrictionColors.Surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(FrictionColors.Surface2, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📱", fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = FrictionColors.OnBackground
                )
                Text(
                    text = "${app.frictionMode.displayName} · ${if (app.isEnabled) "Active" else "Paused"}",
                    fontSize = 11.sp,
                    color = FrictionColors.Muted
                )
            }

            Switch(
                checked = app.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = FrictionColors.Accent,
                    uncheckedThumbColor = FrictionColors.Muted,
                    uncheckedTrackColor = FrictionColors.Surface2
                )
            )
        }
    }
}

@Composable
fun AccessibilityWarningCard(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x14FFAA00)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFAA00)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Accessibility Service Disabled",
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = Color(0xFFFFAA00)
                )
                Text(
                    text = "Tap to enable — required for app interception",
                    fontSize = 11.sp,
                    color = FrictionColors.Muted
                )
            }
        }
    }
}

@Composable
fun EmptyState(onAdd: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
    ) {
        Text("🎯", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No apps protected yet",
            fontSize = 16.sp,
            color = FrictionColors.OnBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your first doomscrolling app\nand start reclaiming your time.",
            fontSize = 13.sp,
            color = FrictionColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = FrictionColors.Accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("+ Add First App", color = Color.Black, fontFamily = FontFamily.Monospace)
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val serviceName = "${context.packageName}/.accessibility.FrictionAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(serviceName)
}

val FrictionMode.displayName: String get() = when (this) {
    FrictionMode.BREATHING -> "Breathing"
    FrictionMode.TYPING    -> "Typing"
    FrictionMode.MATH      -> "Math Mode"
    FrictionMode.WALK      -> "Walk Mode"
    FrictionMode.STRICT    -> "Strict Mode"
}
