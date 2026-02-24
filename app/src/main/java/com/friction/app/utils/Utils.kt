package com.friction.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.friction.app.data.model.ProtectedApp
import java.util.Calendar

/**
 * Passive-aggressive messages shown after 5+ opens in an hour.
 * These are the VIRAL feature. Make them shareable.
 */
object RoastMessages {
    private val messages = listOf(
        "Don't you have code to push? Your TikTok engagement is not going to help your FAANG application.",
        "This is your 6th visit in an hour. The app hasn't changed. Your life hasn't either. Open Android Studio instead.",
        "Congratulations. You've now spent more time on Instagram today than sleeping. Peak optimization.",
        "The people making this app are laughing at you right now. So are the people making TikTok.",
        "Your screen time report this week is going to be humiliating. Just saying.",
        "In the time you've spent here today, you could have shipped a side project. Or at least started one.",
        "Breaking: Local developer spends 3 hours on social media instead of building their 'billion dollar app idea.'",
        "This dopamine hit will last 4 seconds. Your unfinished side project will haunt you for years.",
        "You set up this app because you knew you'd do exactly this. Future you is not impressed.",
        "Every senior engineer who interviewed you can see your public GitHub. It's... not updated.",
    )

    fun getRandom(): String = messages.random()
}

/**
 * Tracks app open counts locally using SharedPreferences.
 * Lightweight alternative to a full DB for hot-path queries.
 */
class UsageTracker(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("usage_tracker", Context.MODE_PRIVATE)

    fun recordOpen(packageName: String) {
        val key = "opens_${packageName}"
        val existing = prefs.getString(key, "") ?: ""
        val now = System.currentTimeMillis().toString()
        // Append timestamp, keep only last 100
        val entries = (existing.split(",") + now)
            .filter { it.isNotBlank() }
            .takeLast(100)
        prefs.edit().putString(key, entries.joinToString(",")).apply()
    }

    fun getOpensInLastHour(packageName: String): Int {
        val key = "opens_${packageName}"
        val existing = prefs.getString(key, "") ?: return 0
        val oneHourAgo = System.currentTimeMillis() - 3_600_000L
        return existing.split(",")
            .filter { it.isNotBlank() }
            .count { it.toLongOrNull()?.let { t -> t > oneHourAgo } == true }
    }
}

/**
 * Checks if Strict Mode is currently active for a given protected app.
 */
class ScheduleChecker {
    fun isStrictModeActive(app: ProtectedApp): Boolean {
        if (!app.strictModeEnabled) return false

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK).toString()

        val isInTimeWindow = hour >= app.strictModeStartHour && hour < app.strictModeEndHour
        val isOnActiveDay = app.strictModeDays.split(",").contains(dayOfWeek)

        return isInTimeWindow && isOnActiveDay
    }
}

/**
 * Boot receiver - ensures the accessibility service restarts after device reboot.
 */
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action == android.content.Intent.ACTION_BOOT_COMPLETED) {
            // The AccessibilityService is managed by Android directly
            // if the user has granted it in Settings - no extra work needed.
            // This receiver is a placeholder for any future startup logic.
        }
    }
}
