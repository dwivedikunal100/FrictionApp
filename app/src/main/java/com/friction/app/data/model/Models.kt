package com.friction.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Friction modes available to protect an app.
 * FREE tier: BREATHING only.
 * PREMIUM: MATH, WALK, STRICT (no bypass).
 */
enum class FrictionMode {
    BREATHING,   // 5-second breathing animation (FREE)
    TYPING,      // Type a humiliating sentence (FREE)
    MATH,        // Solve a math equation (PREMIUM)
    WALK,        // Walk 50 steps (PREMIUM)
    STRICT       // No bypass allowed during scheduled hours (PREMIUM)
}

/**
 * A "protected app" record stored in Room.
 */
@Entity(tableName = "protected_apps")
data class ProtectedApp(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val iconResId: Int = 0,
    val frictionMode: FrictionMode = FrictionMode.BREATHING,
    val isEnabled: Boolean = true,

    // Scheduling (PREMIUM)
    val strictModeEnabled: Boolean = false,
    val strictModeStartHour: Int = 9,   // 9 AM
    val strictModeEndHour: Int = 17,    // 5 PM
    val strictModeDays: String = "1,2,3,4,5" // Mon-Fri (Calendar.DAY_OF_WEEK)
)

/**
 * An interception event - logged every time the user tries to open a protected app.
 */
@Entity(tableName = "interception_events")
data class InterceptionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wasAllowed: Boolean = false,   // Did user get through?
    val timeSpentOnWall: Long = 0,     // ms spent on friction wall
    val frictionMode: FrictionMode = FrictionMode.BREATHING
)
