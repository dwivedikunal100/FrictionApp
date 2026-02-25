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
import kotlinx.coroutines.launch

/**
 * The heart of Friction. This service runs in the background and intercepts
 * app launches by monitoring window state changes. When a "protected" app
 * is opened, it fires the FrictionWallActivity on top of it.
 *
 * Why AccessibilityService? It's the only reliable Android API that can
 * detect foreground app changes without polling (unlike UsageStatsManager).
 */
class FrictionAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: AppRepository
    private lateinit var usageTracker: UsageTracker
    private lateinit var scheduleChecker: ScheduleChecker

    // Track the last intercepted package to avoid double-fired
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0

    // Currently active package in foreground
    private var currentForegroundPackage: String? = null
    
    // Default launcher package
    private var launcherPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = AppRepository.getInstance(applicationContext)
        usageTracker = UsageTracker(applicationContext)
        scheduleChecker = ScheduleChecker()
        
        // Find the default launcher
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        launcherPackage = resolveInfo?.activityInfo?.packageName
    }

    // Track if we are currently showing a wall to avoid multiple launches
    private var isWallActive = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        android.util.Log.d("FrictionService", "Window state changed: $packageName")

        // If we switched to a DIFFERENT package
        if (packageName != currentForegroundPackage) {
            android.util.Log.d("FrictionService", "App switch detected: $currentForegroundPackage -> $packageName")
            
            // If we are moving to our own Wall, don't clear anything
            if (packageName == this.packageName) {
                android.util.Log.d("FrictionService", "Switching to Friction Wall. Keeping allowed states.")
                isWallActive = true
            } else {
                // We left the previous app or the Wall
                if (packageName == launcherPackage) {
                    android.util.Log.d("FrictionService", "Returned to Home. Clearing all allowed states.")
                    clearAllAllowedPackages()
                } else if (currentForegroundPackage != this.packageName && currentForegroundPackage != null) {
                    // We switched from one app to another (neither is Friction or Launcher)
                    android.util.Log.d("FrictionService", "Switched from $currentForegroundPackage to $packageName. Clearing allowed state.")
                    clearAllowedPackage(currentForegroundPackage!!)
                }
                isWallActive = false
            }
            currentForegroundPackage = packageName
        }

        // Ignore our own app for the rest of the logic
        if (packageName == this.packageName) return

        // If a wall is active, don't try to launch another one even if events come from the background app
        if (isWallActive) {
            android.util.Log.d("FrictionService", "Wall is currently active. Ignoring event for $packageName")
            return
        }

        val now = System.currentTimeMillis()

        // Check grace period
        if (isPackageAllowed(packageName)) {
            android.util.Log.d("FrictionService", "Package $packageName is currently allowed (grace period)")
            return
        }

        // Debounce: don't intercept same app twice within 2 seconds
        if (packageName == lastInterceptedPackage && (now - lastInterceptTime) < 2000) {
            android.util.Log.d("FrictionService", "Debounce hit for $packageName")
            return
        }

        serviceScope.launch {
            val protectedApp = repository.getProtectedApp(packageName)
            if (protectedApp == null) {
                android.util.Log.d("FrictionService", "Package $packageName is NOT protected")
                return@launch
            }

            android.util.Log.d("FrictionService", "Intercepting protected app: $packageName")

            // Check if strict mode schedule is blocking this app
            val isStrictModeActive = scheduleChecker.isStrictModeActive(protectedApp)
            android.util.Log.d("FrictionService", "Strict mode active: $isStrictModeActive")

            // Count opens today for incremental timer
            val opensToday = repository.getOpensToday(packageName)
            android.util.Log.d("FrictionService", "Opens today for $packageName: $opensToday")

            // Count opens in the last hour for roast mode
            val opensInLastHour = usageTracker.getOpensInLastHour(packageName)

            // Record this interception attempt (local tracker for roast mode)
            usageTracker.recordOpen(packageName)

            lastInterceptedPackage = packageName
            lastInterceptTime = now

            android.util.Log.d("FrictionService", "Launching Friction Wall for $packageName with ${5 + (opensToday * 5)}s timer")

            // Launch the friction wall
            val intent = Intent(applicationContext, FrictionWallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(FrictionWallActivity.EXTRA_TARGET_PACKAGE, packageName)
                putExtra(FrictionWallActivity.EXTRA_APP_NAME, protectedApp.displayName)
                putExtra(FrictionWallActivity.EXTRA_FRICTION_MODE, protectedApp.frictionMode.name)
                putExtra(FrictionWallActivity.EXTRA_IS_STRICT_MODE, isStrictModeActive)
                putExtra(FrictionWallActivity.EXTRA_OPENS_IN_HOUR, opensInLastHour)
                putExtra(FrictionWallActivity.EXTRA_OPENS_TODAY, opensToday)
            }
            applicationContext.startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // Service interrupted (e.g. user disabled it)
    }

    companion object {
        private val allowedPackagesStatic = mutableMapOf<String, Long>()
        
        fun allowPackage(packageName: String) {
            allowedPackagesStatic[packageName] = System.currentTimeMillis() + 5 * 60 * 1000L
        }
        
        fun isPackageAllowed(packageName: String): Boolean {
            val allowedUntil = allowedPackagesStatic[packageName] ?: 0
            return System.currentTimeMillis() < allowedUntil
        }

        fun clearAllowedPackage(packageName: String) {
            allowedPackagesStatic.remove(packageName)
        }

        fun clearAllAllowedPackages() {
            allowedPackagesStatic.clear()
        }
    }
}
