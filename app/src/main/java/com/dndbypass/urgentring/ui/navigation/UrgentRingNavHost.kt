package com.dndbypass.urgentring.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dndbypass.urgentring.ui.activitylog.ActivityLogScreen
import com.dndbypass.urgentring.ui.dashboard.DashboardScreen
import com.dndbypass.urgentring.ui.debuglog.DebugLogScreen
import com.dndbypass.urgentring.ui.help.HelpFeedbackScreen
import com.dndbypass.urgentring.ui.permissions.PermissionsScreen
import com.dndbypass.urgentring.ui.rules.EditRuleScreen
import com.dndbypass.urgentring.ui.rules.RulesListScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val RULES = "rules"
    const val EDIT_RULE = "rules/edit/{ruleKey}"
    const val ACTIVITY_LOG = "activity"
    const val PERMISSIONS = "permissions"
    const val DEBUG_LOG = "debug_log"
    const val HELP_FEEDBACK = "help_feedback"

    fun editRule(ruleKey: String) = "rules/edit/$ruleKey"
}

@Composable
fun UrgentRingNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenRules = { navController.navigate(Routes.RULES) },
                onOpenActivityLog = { navController.navigate(Routes.ACTIVITY_LOG) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenDebugLog = { navController.navigate(Routes.DEBUG_LOG) },
                onOpenHelpFeedback = { navController.navigate(Routes.HELP_FEEDBACK) }
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
        composable(Routes.DEBUG_LOG) {
            DebugLogScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HELP_FEEDBACK) {
            HelpFeedbackScreen(onBack = { navController.popBackStack() })
        }
    }
}
