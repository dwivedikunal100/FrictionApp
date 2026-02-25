package com.friction.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.friction.app.billing.FrictionBillingManager
import com.friction.app.data.repository.AppRepository
import com.friction.app.ui.screens.*
import com.friction.app.ui.theme.FrictionTheme
import com.friction.app.utils.PermissionUtils
import com.friction.app.utils.PreferenceManager

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: FrictionBillingManager
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        billingManager = FrictionBillingManager(this)
        billingManager.initialize()
        
        preferenceManager = PreferenceManager(this)

        val repository = AppRepository.getInstance(this)

        setContent {
            FrictionTheme {
                FrictionApp(
                    repository = repository,
                    billingManager = billingManager,
                    preferenceManager = preferenceManager,
                    activity = this
                )
            }
        }
    }
}

@Composable
fun FrictionApp(
    repository: AppRepository,
    billingManager: FrictionBillingManager,
    preferenceManager: PreferenceManager,
    activity: MainActivity
) {
    val navController = rememberNavController()
    val isPremium by billingManager.isPremium.collectAsState()
    
    val context = LocalContext.current
    val startDestination = if (preferenceManager.isFirstLaunch || 
        !PermissionUtils.isAccessibilityServiceEnabled(context) || 
        !PermissionUtils.isUsageStatsEnabled(context)) {
        "permissions"
    } else {
        "home"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("permissions") {
            PermissionsScreen(
                onPermissionsGranted = {
                    preferenceManager.isFirstLaunch = false
                    navController.navigate("home") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            val viewModel = remember { HomeViewModel(repository) }
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAddApp = { navController.navigate("add_app") },
                onNavigateToPaywall = { navController.navigate("paywall") }
            )
        }

        composable("paywall") {
            PaywallScreen(
                onSubscribe = { isAnnual ->
                    billingManager.openPlayStore()
                },
                onDismiss = { navController.popBackStack() }
            )
        }

        composable("add_app") {
            AddAppScreen(
                repository = repository,
                isPremium = isPremium,
                onAppAdded = { navController.popBackStack() },
                onNeedsUpgrade = { navController.navigate("paywall") }
            )
        }
    }
}
