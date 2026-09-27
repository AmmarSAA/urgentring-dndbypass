package com.dndbypass.urgentring

import android.app.Application
import com.dndbypass.urgentring.data.AppDatabase
import com.dndbypass.urgentring.data.CallerRule
import com.dndbypass.urgentring.data.DEFAULT_RULE_KEY
import com.dndbypass.urgentring.data.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UrgentRingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // A fresh "process started" log line lets us tell, from the log export alone,
        // whether the OS killed and respawned the app between test calls (a common
        // MIUI/aggressive-battery-manager failure mode) rather than the app just
        // silently failing to act while already running.
        DebugLogger.log(this, "Application process started")

        // Seeded here (not in a screen's ViewModel) so the default rule exists for the
        // CallScreeningService even if the user never opens the Rules screen.
        CoroutineScope(Dispatchers.IO).launch {
            val ruleDao = AppDatabase.get(this@UrgentRingApplication).callerRuleDao()
            val existingDefault = ruleDao.findByKey(DEFAULT_RULE_KEY)
            if (existingDefault == null) {
                ruleDao.upsert(
                    CallerRule(
                        numberOrDefault = DEFAULT_RULE_KEY,
                        displayName = "All callers",
                        thresholdCalls = 3,
                        windowMinutes = 10,
                        enabled = true
                    )
                )
                DebugLogger.log(this@UrgentRingApplication, "Seeded default rule (none existed)")
            } else {
                DebugLogger.log(this@UrgentRingApplication, "Default rule already present: $existingDefault")
            }
        }
    }
}
