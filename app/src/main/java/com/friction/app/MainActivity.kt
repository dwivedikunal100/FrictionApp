package com.friction.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.friction.app.billing.FrictionBillingManager
import com.friction.app.data.repository.AppRepository
import com.friction.app.ui.screens.*
import com.friction.app.ui.theme.FrictionTheme

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: FrictionBillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        billingManager = FrictionBillingManager(this)
        billingManager.initialize()

        val repository = AppRepository.getInstance(this)

        setContent {
            FrictionTheme {
                FrictionApp(
                    repository = repository,
                    billingManager = billingManager,
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
    activity: MainActivity
) {
    val navController = rememberNavController()
    val isPremium by billingManager.isPremium.collectAsState()

    NavHost(navController = navController, startDestination = "home") {

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
                    val products = billingManager.products.value
                    val productId = if (isAnnual)
                        FrictionBillingManager.PRODUCT_ANNUAL
                    else
                        FrictionBillingManager.PRODUCT_MONTHLY
                    val product = products.firstOrNull { it.productId == productId }
                    if (product != null) {
                        billingManager.launchBillingFlow(activity, product, isAnnual)
                    }
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
