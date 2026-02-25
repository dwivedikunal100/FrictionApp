package com.friction.app.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friction.app.data.model.FrictionMode
import com.friction.app.data.model.ProtectedApp
import com.friction.app.data.repository.AppRepository
import com.friction.app.ui.theme.FrictionColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String)

@Composable
fun AddAppScreen(
        repository: AppRepository,
        isPremium: Boolean,
        onAppAdded: () -> Unit,
        onNeedsUpgrade: () -> Unit
) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var query by remember { mutableStateOf("") }
        var allApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
        var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
        var selectedMode by remember { mutableStateOf(FrictionMode.BREATHING) }

        // Load installed apps
        LaunchedEffect(Unit) {
                val protectedPackages =
                        repository.getAllApps().first().map { it.packageName }.toSet()
                allApps =
                        withContext(Dispatchers.IO) {
                                val pm = context.packageManager
                                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                                        .filter { appInfo ->
                                                // Only include apps that the user can actually
                                                // launch
                                                // and haven't already been protected
                                                pm.getLaunchIntentForPackage(appInfo.packageName) !=
                                                        null &&
                                                        appInfo.packageName !=
                                                                context.packageName &&
                                                        !protectedPackages.contains(
                                                                appInfo.packageName
                                                        )
                                        }
                                        .map { appInfo ->
                                                InstalledApp(
                                                        appInfo.packageName,
                                                        appInfo.loadLabel(pm).toString()
                                                )
                                        }
                                        .sortedBy { it.label }
                        }
        }

        val filtered =
                allApps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .background(FrictionColors.Background)
                                .padding(horizontal = 24.dp)
        ) {
                Spacer(modifier = Modifier.height(56.dp))

                Text(
                        text = "ADD APP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 4.sp,
                        color = FrictionColors.Muted
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "Choose an app\nto protect.",
                        fontSize = 28.sp,
                        color = FrictionColors.OnBackground,
                        lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Search field
                OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search apps...", color = FrictionColors.Muted) },
                        leadingIcon = {
                                Icon(Icons.Default.Search, null, tint = FrictionColors.Muted)
                        },
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FrictionColors.Accent,
                                        unfocusedBorderColor = FrictionColors.Border,
                                        focusedTextColor = FrictionColors.OnBackground,
                                        unfocusedTextColor = FrictionColors.OnBackground
                                ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // If an app is selected, show friction mode picker
                if (selectedApp != null) {
                        FrictionModePicker(
                                selected = selectedMode,
                                isPremium = isPremium,
                                onSelect = { mode ->
                                        if ((mode == FrictionMode.MATH ||
                                                        mode == FrictionMode.WALK ||
                                                        mode == FrictionMode.STRICT) && !isPremium
                                        ) {
                                                onNeedsUpgrade()
                                        } else {
                                                selectedMode = mode
                                        }
                                }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                                onClick = {
                                        scope.launch {
                                                repository.addApp(
                                                        ProtectedApp(
                                                                packageName =
                                                                        selectedApp!!.packageName,
                                                                displayName = selectedApp!!.label,
                                                                frictionMode = selectedMode
                                                        )
                                                )
                                                onAppAdded()
                                        }
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = FrictionColors.Accent
                                        ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                                Text(
                                        "PROTECT ${selectedApp!!.label.uppercase()} →",
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                                AppPickerRow(
                                        app = app,
                                        isSelected = selectedApp?.packageName == app.packageName,
                                        onClick = {
                                                selectedApp = if (selectedApp == app) null else app
                                        }
                                )
                        }
                }
        }
}

@Composable
fun AppPickerRow(app: InstalledApp, isSelected: Boolean, onClick: () -> Unit) {
        Card(
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        if (isSelected) FrictionColors.AccentDim
                                        else FrictionColors.Surface
                        ),
                shape = RoundedCornerShape(12.dp),
                border =
                        if (isSelected)
                                androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        FrictionColors.Accent
                                )
                        else null,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        ) {
                Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Box(
                                modifier =
                                        Modifier.size(40.dp)
                                                .background(
                                                        FrictionColors.Surface2,
                                                        RoundedCornerShape(10.dp)
                                                ),
                                contentAlignment = Alignment.Center
                        ) { Text("📱", fontSize = 20.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(app.label, fontSize = 14.sp, color = FrictionColors.OnBackground)
                        if (isSelected) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text("✓", color = FrictionColors.Accent, fontSize = 16.sp)
                        }
                }
        }
}

@Composable
fun FrictionModePicker(
        selected: FrictionMode,
        isPremium: Boolean,
        onSelect: (FrictionMode) -> Unit
) {
        val modes =
                listOf(
                        FrictionMode.BREATHING to ("🫁" to false),
                        FrictionMode.TYPING to ("✍️" to false),
                        FrictionMode.MATH to ("🧮" to true),
                        FrictionMode.WALK to ("🚶" to true),
                        FrictionMode.STRICT to ("🔒" to true),
                )

        Column {
                Text(
                        text = "FRICTION MODE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 3.sp,
                        color = FrictionColors.Muted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { (mode, pair) ->
                                val (emoji, isPro) = pair
                                val locked = isPro && !isPremium
                                ModeChip(
                                        emoji = emoji,
                                        label = mode.displayName,
                                        isSelected = selected == mode,
                                        isLocked = locked,
                                        onClick = { onSelect(mode) }
                                )
                        }
                }
        }
}

@Composable
fun ModeChip(
        emoji: String,
        label: String,
        isSelected: Boolean,
        isLocked: Boolean,
        onClick: () -> Unit
) {
        Card(
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        if (isSelected) FrictionColors.AccentDim
                                        else FrictionColors.Surface
                        ),
                border =
                        if (isSelected)
                                androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        FrictionColors.Accent
                                )
                        else null,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable(onClick = onClick)
        ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                        Text(if (isLocked) "🔒" else emoji, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = label.split(" ").first(),
                                fontSize = 9.sp,
                                color =
                                        if (isSelected) FrictionColors.Accent
                                        else FrictionColors.Muted,
                                fontFamily = FontFamily.Monospace
                        )
                }
        }
}
