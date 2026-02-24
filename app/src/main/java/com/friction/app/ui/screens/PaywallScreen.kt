package com.friction.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friction.app.ui.theme.FrictionColors

@Composable
fun PaywallScreen(
    timeSavedHours: Float = 2.4f,
    onSubscribe: (isAnnual: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var isAnnual by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FrictionColors.Background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // Close button
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(FrictionColors.Surface, RoundedCornerShape(50))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text("×", fontSize = 20.sp, color = FrictionColors.Muted)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Header
        Text(
            text = "YOU SAVED ${"%.1f".format(timeSavedHours)}HRS TODAY",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 3.sp,
            color = FrictionColors.Accent
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Unlock hardcore\nfriction.",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = FrictionColors.OnBackground,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "The free tier got you started. Go further.",
            fontSize = 14.sp,
            color = FrictionColors.Muted
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Feature list
        PremiumFeatures()

        Spacer(modifier = Modifier.height(24.dp))

        // Plan selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlanOption(
                name = "MONTHLY",
                price = "$4.99",
                period = "per month",
                isSelected = !isAnnual,
                badge = null,
                modifier = Modifier.weight(1f),
                onClick = { isAnnual = false }
            )
            PlanOption(
                name = "ANNUAL",
                price = "$29.99",
                period = "$2.50 / mo · save 50%",
                isSelected = isAnnual,
                badge = "BEST VALUE",
                modifier = Modifier.weight(1f),
                onClick = { isAnnual = true }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CTA Button
        Button(
            onClick = { onSubscribe(isAnnual) },
            colors = ButtonDefaults.buttonColors(containerColor = FrictionColors.Accent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(58.dp)
        ) {
            Text(
                text = "GO TO GOOGLE PLAY TO PAY →",
                color = Color.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No charge for 3 days. Cancel anytime.",
            fontSize = 12.sp,
            color = FrictionColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isAnnual) "Billed annually at \$29.99. Renews automatically."
            else "Billed monthly at \$4.99. Renews automatically.",
            fontSize = 10.sp,
            color = Color(0xFF444444),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            lineHeight = 16.sp
        )
    }
}

@Composable
fun PremiumFeatures() {
    val features = listOf(
        "Math Mode" to "Solve equations to open any app",
        "Walk Mode" to "50 steps before you doomscroll",
        "Strict Mode" to "Zero bypasses during work hours",
        "Unlimited Apps" to "Block every dopamine tap",
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        features.forEach { (title, subtitle) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(FrictionColors.AccentDim, RoundedCornerShape(50))
                        .border(1.5.dp, FrictionColors.Accent, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 11.sp, color = FrictionColors.Accent)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FrictionColors.OnBackground)
                    Text(subtitle, fontSize = 12.sp, color = FrictionColors.Muted)
                }
            }
        }
    }
}

@Composable
fun PlanOption(
    name: String,
    price: String,
    period: String,
    isSelected: Boolean,
    badge: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) FrictionColors.AccentDim else FrictionColors.Surface
            ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isSelected) FrictionColors.Accent else FrictionColors.Border
            ),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = name,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = FrictionColors.Muted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = price,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) FrictionColors.Accent else FrictionColors.OnBackground
                )
                Text(text = period, fontSize = 10.sp, color = FrictionColors.Muted, lineHeight = 16.sp)
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-10).dp)
                    .background(FrictionColors.Accent, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
