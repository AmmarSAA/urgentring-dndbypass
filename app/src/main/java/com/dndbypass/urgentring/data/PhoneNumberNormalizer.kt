package com.dndbypass.urgentring.data

object PhoneNumberNormalizer {

    /**
     * Strips formatting so the same caller matches regardless of how the number was
     * typed or how the carrier delivers it. MVP-only heuristic (assumes US/NANP for
     * bare 10-digit numbers) — swap for Google's libphonenumber if international
     * numbers matter.
     */
    fun normalize(rawNumber: String?): String? {
        if (rawNumber.isNullOrBlank()) return null
        val digits = rawNumber.filter { it.isDigit() || it == '+' }
        if (digits.isEmpty()) return null
        return when {
            digits.startsWith("+") -> digits
            digits.length == 10 -> "+1$digits"
            else -> "+$digits"
        }
    }
}
