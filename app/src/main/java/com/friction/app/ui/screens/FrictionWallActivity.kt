package com.friction.app.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friction.app.data.model.FrictionMode
import com.friction.app.data.repository.AppRepository
import com.friction.app.ui.theme.*
import com.friction.app.utils.RoastMessages
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FrictionWallActivity
 *
 * This is the overlay that appears when the user opens a protected app.
 * It's launched as a new task so it appears on TOP of the intercepted app.
 * The user must complete the friction challenge before they can proceed.
 *
 * Key design decision: We use launchMode="singleTop" + excludeFromRecents
 * so it doesn't pollute the recents screen.
 */
class FrictionWallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_FRICTION_MODE = "friction_mode"
        const val EXTRA_IS_STRICT_MODE = "is_strict_mode"
        const val EXTRA_OPENS_IN_HOUR = "opens_in_hour"
        const val EXTRA_OPENS_TODAY = "opens_today"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: run { finish(); return }
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: targetPackage
        val frictionMode = intent.getStringExtra(EXTRA_FRICTION_MODE)
            ?.let { FrictionMode.valueOf(it) } ?: FrictionMode.BREATHING
        val isStrictMode = intent.getBooleanExtra(EXTRA_IS_STRICT_MODE, false)
        val opensInHour = intent.getIntExtra(EXTRA_OPENS_IN_HOUR, 0)
        val opensToday = intent.getIntExtra(EXTRA_OPENS_TODAY, 0)

        val wallStartTime = System.currentTimeMillis()
        val roastMessage = if (opensInHour >= 5) RoastMessages.getRandom() else null
        
        // Timer increases by 5s for each open today (5, 10, 15...)
        val timerDuration = 5 + (opensToday * 5)

        setContent {
            FrictionTheme {
                FrictionWallScreen(
                    appName = appName,
                    frictionMode = frictionMode,
                    isStrictMode = isStrictMode,
                    opensInHour = opensInHour,
                    roastMessage = roastMessage,
                    initialTimerSeconds = timerDuration,
                    onAllowAccess = {
                        val timeSpent = System.currentTimeMillis() - wallStartTime
                        // Allow for 5 minutes
                        com.friction.app.accessibility.FrictionAccessibilityService.allowPackage(targetPackage)
                        finish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun FrictionWallScreen(
    appName: String,
    frictionMode: FrictionMode,
    isStrictMode: Boolean,
    opensInHour: Int,
    roastMessage: String?,
    initialTimerSeconds: Int,
    onAllowAccess: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FrictionColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // "Intercepted" label
            Text(
                text = "INTERCEPTED",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 4.sp,
                color = FrictionColors.Muted
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App name + heading
            Text(
                text = "Hold on,",
                fontSize = 28.sp,
                color = FrictionColors.OnBackground,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "really?",
                fontSize = 28.sp,
                color = FrictionColors.Accent,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You've opened $appName\n$opensInHour times in the last hour.",
                fontSize = 14.sp,
                color = FrictionColors.Muted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // The friction challenge
            when {
                isStrictMode -> StrictModeBlock()
                frictionMode == FrictionMode.BREATHING -> BreathingChallenge(seconds = initialTimerSeconds, onComplete = onAllowAccess)
                frictionMode == FrictionMode.MATH -> MathChallenge(onComplete = onAllowAccess)
                frictionMode == FrictionMode.TYPING -> TypingChallenge(onComplete = onAllowAccess)
                else -> BreathingChallenge(seconds = initialTimerSeconds, onComplete = onAllowAccess)
            }

            // Roast message (if opened too many times)
            if (roastMessage != null) {
                Spacer(modifier = Modifier.height(24.dp))
                RoastCard(message = roastMessage)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Skip (lets user through, just records it)
            if (!isStrictMode) {
                Text(
                    text = "I don't care, let me in →",
                    fontSize = 12.sp,
                    color = FrictionColors.Muted,
                    modifier = Modifier.clickable { onAllowAccess() }
                )
            }
        }
    }
}

// ── Breathing Challenge ──────────────────────────────────────────────────────

@Composable
fun BreathingChallenge(seconds: Int, onComplete: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(seconds) }
    val breatheScale by rememberInfiniteTransition(label = "breathe").animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        onComplete()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(breatheScale)
                    .alpha(0.2f)
                    .border(1.5.dp, FrictionColors.Accent, CircleShape)
            )
            // Middle ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(breatheScale)
                    .alpha(0.4f)
                    .border(1.5.dp, FrictionColors.Accent, CircleShape)
            )
            // Inner filled circle with countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(FrictionColors.AccentDim, CircleShape)
                    .border(1.5.dp, FrictionColors.Accent, CircleShape)
            ) {
                Text(
                    text = secondsLeft.toString(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    color = FrictionColors.Accent,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "BREATHE IN · HOLD · OUT",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            color = FrictionColors.Muted
        )
    }
}

// ── Math Challenge ────────────────────────────────────────────────────────────

@Composable
fun MathChallenge(onComplete: () -> Unit) {
    // Generate a random medium-difficulty equation
    val (a, b, answer) = remember {
        val pairs = listOf(
            Triple(14, 8, 112), Triple(17, 6, 102), Triple(23, 7, 161),
            Triple(9, 13, 117), Triple(15, 12, 180), Triple(8, 19, 152)
        )
        pairs.random()
    }

    var userInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Equation card
        Card(
            colors = CardDefaults.cardColors(containerColor = FrictionColors.Surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Text(
                    text = "SOLVE TO CONTINUE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 3.sp,
                    color = FrictionColors.Muted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$a × ",
                    fontSize = 40.sp,
                    fontFamily = FontFamily.Monospace,
                    color = FrictionColors.OnBackground,
                    style = MaterialTheme.typography.displayMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$b",
                        fontSize = 40.sp,
                        fontFamily = FontFamily.Monospace,
                        color = FrictionColors.Accent
                    )
                    Text(
                        text = " = ?",
                        fontSize = 40.sp,
                        fontFamily = FontFamily.Monospace,
                        color = FrictionColors.OnBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input field
        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it.filter { c -> c.isDigit() } },
            placeholder = { Text("Your answer", color = FrictionColors.Muted) },
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FrictionColors.Accent,
                unfocusedBorderColor = FrictionColors.Border,
                errorBorderColor = Color(0xFFFF4D4D),
                focusedTextColor = FrictionColors.OnBackground,
                unfocusedTextColor = FrictionColors.OnBackground,
                cursorColor = FrictionColors.Accent
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (userInput.toIntOrNull() == answer) {
                    onComplete()
                } else {
                    isError = true
                    scope.launch {
                        delay(800)
                        isError = false
                        userInput = ""
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = FrictionColors.Accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                text = "SUBMIT →",
                color = Color.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}

// ── Typing Challenge ─────────────────────────────────────────────────────────

@Composable
fun TypingChallenge(onComplete: () -> Unit) {
    val phrase = "I am choosing to waste my time"
    var userInput by remember { mutableStateOf("") }

    LaunchedEffect(userInput) {
        if (userInput.trim().equals(phrase, ignoreCase = true)) {
            delay(400)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            colors = CardDefaults.cardColors(containerColor = FrictionColors.Surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "\"$phrase\"",
                fontSize = 16.sp,
                color = FrictionColors.OnBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp),
                lineHeight = 26.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "TYPE THE ABOVE TO CONTINUE",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = FrictionColors.Muted
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            placeholder = { Text("Start typing...", color = FrictionColors.Muted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FrictionColors.Accent,
                unfocusedBorderColor = FrictionColors.Border,
                focusedTextColor = FrictionColors.OnBackground,
                unfocusedTextColor = FrictionColors.OnBackground
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// ── Strict Mode Block ─────────────────────────────────────────────────────────

@Composable
fun StrictModeBlock() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x14FF4D4D)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FF4D4D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "🔒", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "STRICT MODE ACTIVE",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = Color(0xFFFF4D4D)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You cannot open this app during work hours.\nYou set this rule. We're enforcing it.",
                fontSize = 13.sp,
                color = FrictionColors.Muted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ── Roast Card ────────────────────────────────────────────────────────────────

@Composable
fun RoastCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x14FF4D4D)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FF4D4D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "// ROAST MODE",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 3.sp,
                color = Color(0xFFFF4D4D)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color(0xFFFF7070),
                lineHeight = 20.sp
            )
        }
    }
}
