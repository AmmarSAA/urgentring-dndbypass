package com.dndbypass.urgentring.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dndbypass.urgentring.data.SettingsRepository
import com.dndbypass.urgentring.ui.activitylog.ActivityLogScreen
import com.dndbypass.urgentring.ui.dashboard.DashboardScreen
import com.dndbypass.urgentring.ui.debuglog.DebugLogScreen
import com.dndbypass.urgentring.ui.donate.DonateScreen
import com.dndbypass.urgentring.ui.help.HelpFeedbackScreen
import com.dndbypass.urgentring.ui.locationshare.LocationShareScreen
import com.dndbypass.urgentring.ui.onboarding.OnboardingScreen
import com.dndbypass.urgentring.ui.permissions.PermissionsScreen
import com.dndbypass.urgentring.ui.rules.EditRuleScreen
import com.dndbypass.urgentring.ui.rules.RulesListScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val RULES = "rules"
    const val EDIT_RULE = "rules/edit/{ruleKey}"
    const val ACTIVITY_LOG = "activity"
    const val PERMISSIONS = "permissions"
    const val DEBUG_LOG = "debug_log"
    const val HELP_FEEDBACK = "help_feedback"
    const val DONATE = "donate"
    const val LOCATION_SHARE = "location_share"

    fun editRule(ruleKey: String) = "rules/edit/$ruleKey"
}

@Composable
fun UrgentRingNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val settingsRepository = remember(context) { SettingsRepository(context) }
    val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = null)

    // Wait for the real value before picking a start destination, so a first-time user
    // never flashes the Dashboard before landing on Onboarding.
    val startDestination = onboardingCompleted ?: return

    NavHost(
        navController = navController,
        startDestination = if (startDestination) Routes.DASHBOARD else Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenRules = { navController.navigate(Routes.RULES) },
                onOpenActivityLog = { navController.navigate(Routes.ACTIVITY_LOG) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenLocationShare = { navController.navigate(Routes.LOCATION_SHARE) },
                onOpenDebugLog = { navController.navigate(Routes.DEBUG_LOG) },
                onOpenHelpFeedback = { navController.navigate(Routes.HELP_FEEDBACK) },
                onOpenDonate = { navController.navigate(Routes.DONATE) }
            )
        }
        composable(Routes.RULES) {
            RulesListScreen(
                onAddRule = { navController.navigate(Routes.editRule("new")) },
                onEditRule = { key -> navController.navigate(Routes.editRule(key)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.EDIT_RULE) { backStackEntry ->
            val ruleKey = backStackEntry.arguments?.getString("ruleKey") ?: "new"
            EditRuleScreen(ruleKey = ruleKey, onDone = { navController.popBackStack() })
        }
        composable(Routes.ACTIVITY_LOG) {
            ActivityLogScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LOCATION_SHARE) {
            LocationShareScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DEBUG_LOG) {
            DebugLogScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HELP_FEEDBACK) {
            HelpFeedbackScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DONATE) {
            DonateScreen(onBack = { navController.popBackStack() })
        }
    }
}
