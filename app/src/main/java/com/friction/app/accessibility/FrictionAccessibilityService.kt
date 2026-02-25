package com.friction.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.friction.app.data.repository.AppRepository
import com.friction.app.ui.screens.FrictionWallActivity
import com.friction.app.utils.ScheduleChecker
import com.friction.app.utils.UsageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * The heart of Friction. This service runs in the background and intercepts app launches by
 * monitoring window state changes. When a "protected" app is opened, it fires the
 * FrictionWallActivity on top of it.
 *
 * Why AccessibilityService? It's the only reliable Android API that can detect foreground app
 * changes without polling (unlike UsageStatsManager).
 */
class FrictionAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: AppRepository
    private lateinit var usageTracker: UsageTracker
    private lateinit var scheduleChecker: ScheduleChecker

    // In-memory cache for instantaneous lookups
    private val protectedAppsCache =
            mutableMapOf<String, com.friction.app.data.model.ProtectedApp>()

    // Track the last intercepted package for debouncing
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0

    // Currently active package in foreground
    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = AppRepository.getInstance(applicationContext)
        usageTracker = UsageTracker(applicationContext)
        scheduleChecker = ScheduleChecker()

        // Keep the cache synced with the database
        serviceScope.launch {
            repository.getAllApps().collect { apps ->
                val activeApps = apps.filter { it.isEnabled }
                protectedAppsCache.clear()
                activeApps.forEach { protectedAppsCache[it.packageName] = it }
                android.util.Log.d(
                        "FrictionService",
                        "Cache updated: ${protectedAppsCache.keys.size} apps"
                )
            }
        }
    }

    // Track if we are currently showing a wall to avoid multiple launches
    private var isWallActive = false

    private fun isLauncher(pkg: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfos =
                packageManager.queryIntentActivities(
                        intent,
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                )
        return resolveInfos.any { it.activityInfo?.packageName == pkg }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        android.util.Log.d("FrictionService", "Window state changed: $packageName")

        // If we switched to a DIFFERENT package
        if (packageName != currentForegroundPackage) {
            android.util.Log.d(
                    "FrictionService",
                    "App switch detected: $currentForegroundPackage -> $packageName"
            )

            // If we are moving to our own Wall, don't clear anything
            if (packageName == this.packageName) {
                android.util.Log.d(
                        "FrictionService",
                        "Switching to Friction Wall. Keeping allowed states."
                )
                isWallActive = true
            } else {
                // We left the previous app or the Wall
                if (currentForegroundPackage == this.packageName) {
                    android.util.Log.d(
                            "FrictionService",
                            "Transitioning from Wall back to $packageName. Preserving allowed states."
                    )
                } else if (isLauncher(packageName)) {
                    android.util.Log.d(
                            "FrictionService",
                            "Returned to Home ($packageName). Clearing all allowed states."
                    )
                    clearAllAllowedPackages()
                } else if (currentForegroundPackage != null) {
                    android.util.Log.d(
                            "FrictionService",
                            "Switched from $currentForegroundPackage to $packageName. Clearing allowed state for previous app."
                    )
                    clearAllowedPackage(currentForegroundPackage!!)
                }
                isWallActive = false
            }
            currentForegroundPackage = packageName
        }

        // Ignore our own app for the rest of the logic
        if (packageName == this.packageName) return

        // If a wall is active, don't try to launch another one
        if (isWallActive) {
            android.util.Log.d(
                    "FrictionService",
                    "Wall is currently active. Ignoring event for $packageName"
            )
            return
        }

        val now = System.currentTimeMillis()

        // Check grace period (Instant check)
        if (isPackageAllowed(packageName)) {
            android.util.Log.d(
                    "FrictionService",
                    "Package $packageName is currently allowed (grace period)"
            )
            return
        }

        // Debounce: don't intercept same app twice within 2 seconds
        if (packageName == lastInterceptedPackage && (now - lastInterceptTime) < 2000) {
            android.util.Log.d("FrictionService", "Debounce hit for $packageName")
            return
        }

        // SYNC CHECK: Use the cache for zero-delay interception
        val protectedApp = protectedAppsCache[packageName]
        if (protectedApp == null) {
            return
        }

        android.util.Log.d("FrictionService", "Intercepting protected app: $packageName")

        // IMMEDIATE LAUNCH: Don't wait for coroutines to start the activity
        // This ensures the Wall hits BEFORE the target app can render its UI
        val intent =
                Intent(applicationContext, FrictionWallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(FrictionWallActivity.EXTRA_TARGET_PACKAGE, packageName)
                    putExtra(FrictionWallActivity.EXTRA_APP_NAME, protectedApp.displayName)
                    putExtra(
                            FrictionWallActivity.EXTRA_FRICTION_MODE,
                            protectedApp.frictionMode.name
                    )
                    // Note: Some extras like opensToday will be updated in background and passed if
                    // needed,
                    // or fetched by the Activity itself for most accurate data.
                }

        lastInterceptedPackage = packageName
        lastInterceptTime = now
        isWallActive = true // Set immediately to prevent double launch

        // Small delay (increased to 100ms) to ensure the target app settles
        serviceScope.launch(Dispatchers.Main) {
            delay(100)
            startActivity(intent)

            // Failsafe: Reset isWallActive after 5 seconds if it hasn't been reset by other events.
            // This prevents a permanent "locked" state if the Wall activity fails to launch
            // or is immediately killed by the system.
            delay(5000)
            if (isWallActive && currentForegroundPackage != packageName) {
                android.util.Log.w(
                        "FrictionService",
                        "Failsafe: Resetting isWallActive for $packageName"
                )
                isWallActive = false
            }
        }

        // Run non-critical data fetching Tasks in background
        serviceScope.launch {
            // Check if strict mode schedule is blocking this app
            val isStrictModeActive = scheduleChecker.isStrictModeActive(protectedApp)

            // Count opens today for incremental timer
            val opensToday = repository.getOpensToday(packageName)

            // Count opens in the last hour for roast mode
            val opensInLastHour = usageTracker.getOpensInLastHour(packageName)

            // Record this interception attempt
            usageTracker.recordOpen(packageName)

            android.util.Log.d(
                    "FrictionService",
                    "Background tasks completed for $packageName. Opens: $opensToday, Strict: $isStrictModeActive"
            )

            // Note: In a more robust implementation, we might send an update to the Wall
            // if strict mode status or open counts change what should be displayed.
            // For now, immediate launch with base info is the priority.
        }
    }

    override fun onInterrupt() {
        // Service interrupted (e.g. user disabled it)
    }

    companion object {
        private val allowedPackagesStatic = mutableMapOf<String, Long>()

        fun allowPackage(packageName: String) {
            android.util.Log.d("FrictionService", "Allowing package: $packageName for 5 minutes")
            allowedPackagesStatic[packageName] = System.currentTimeMillis() + 5 * 60 * 1000L
        }

        fun isPackageAllowed(packageName: String): Boolean {
            val allowedUntil = allowedPackagesStatic[packageName] ?: 0
            val isAllowed = System.currentTimeMillis() < allowedUntil
            return isAllowed
        }

        fun clearAllowedPackage(packageName: String) {
            android.util.Log.d("FrictionService", "Clearing allowed state for: $packageName")
            allowedPackagesStatic.remove(packageName)
        }

        fun clearAllAllowedPackages() {
            android.util.Log.d("FrictionService", "Clearing ALL allowed states")
            allowedPackagesStatic.clear()
        }
    }
}
