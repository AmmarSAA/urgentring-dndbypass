package com.dndbypass.urgentring.screening

import com.dndbypass.urgentring.data.CallEvent
import com.dndbypass.urgentring.data.CallEventDao
import com.dndbypass.urgentring.data.CallerRule
import com.dndbypass.urgentring.data.CallerRuleDao
import com.dndbypass.urgentring.data.DEFAULT_RULE_KEY
import java.util.concurrent.TimeUnit

// eventId is null when no event was recorded (no rule, disabled, or excluded), and is
// what the caller marks as "rang through" — only after the ringer override actually
// succeeds, not just because the threshold was crossed.
data class ScreeningDecision(val shouldRingAudibly: Boolean, val rule: CallerRule?, val eventId: Long?)

/**
 * Resolves the rule for a caller and counts calls in that rule's rolling window.
 * Counting is per normalized number — every caller keeps an independent count, and
 * calls older than the window are pruned so they stop counting toward the threshold.
 */
class CallCounter(
    private val ruleDao: CallerRuleDao,
    private val eventDao: CallEventDao
) {
    suspend fun recordAndEvaluate(normalizedNumber: String?, nowMillis: Long): ScreeningDecision {
        val rule = resolveRule(normalizedNumber) ?: return ScreeningDecision(false, null, null)
        if (!rule.enabled || rule.neverOverride) return ScreeningDecision(false, rule, null)

        val key = normalizedNumber ?: UNKNOWN_CALLER_KEY
        val windowStart = nowMillis - TimeUnit.MINUTES.toMillis(rule.windowMinutes.toLong())

        eventDao.pruneOlderThan(windowStart)
        val eventId = eventDao.insert(CallEvent(normalizedNumber = key, timestampMillis = nowMillis, ringOverridden = false))

        val countInWindow = eventDao.eventsInWindow(key, windowStart).size
        val shouldRing = countInWindow >= rule.thresholdCalls
        return ScreeningDecision(shouldRingAudibly = shouldRing, rule = rule, eventId = eventId)
    }

    suspend fun markRingOverridden(eventId: Long) {
        eventDao.markRingOverridden(eventId)
    }

    private suspend fun resolveRule(normalizedNumber: String?): CallerRule? {
        if (normalizedNumber != null) {
            ruleDao.findByKey(normalizedNumber)?.let { return it }
        }
        return ruleDao.findByKey(DEFAULT_RULE_KEY)
    }

    companion object {
        const val UNKNOWN_CALLER_KEY = "__unknown__"
    }
}
