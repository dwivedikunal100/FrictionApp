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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app
        if (packageName == "com.friction.app") return

        val now = System.currentTimeMillis()

        // If we switched to a DIFFERENT package
        if (packageName != currentForegroundPackage) {
            // If we went to the launcher or a different app, we should clear the "allowed" state 
            // for the previous app so next time it's opened, it's blocked again.
            if (packageName == launcherPackage) {
                // Clear all allowed packages when returning to home
                clearAllAllowedPackages()
            } else {
                // If we went to another app, clear the previous one
                currentForegroundPackage?.let { clearAllowedPackage(it) }
            }
            currentForegroundPackage = packageName
        }

        // Check grace period
        if (isPackageAllowed(packageName)) return

        // Debounce: don't intercept same app twice within 2 seconds
        if (packageName == lastInterceptedPackage && (now - lastInterceptTime) < 2000) return

        serviceScope.launch {
            val protectedApp = repository.getProtectedApp(packageName) ?: return@launch

            // Check if strict mode schedule is blocking this app
            val isStrictModeActive = scheduleChecker.isStrictModeActive(protectedApp)

            // Count opens today for incremental timer
            val opensToday = repository.getOpensToday(packageName)

            // Count opens in the last hour for roast mode
            val opensInLastHour = usageTracker.getOpensInLastHour(packageName)

            // Record this interception attempt
            usageTracker.recordOpen(packageName)

            lastInterceptedPackage = packageName
            lastInterceptTime = now

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

            // For now, we assume if the wall finishes, it's allowed.
            // In a more robust implementation, the Activity could notify the Service.
            // Since they are in the same process, we can use a static singleton or broadcast.
            // Let's use a simple static map for communication.
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
