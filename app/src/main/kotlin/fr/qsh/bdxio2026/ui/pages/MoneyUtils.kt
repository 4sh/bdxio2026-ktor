package fr.qsh.bdxio2026.ui.pages

import java.math.BigDecimal
import kotlin.math.absoluteValue

fun formatEuros(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val euros = (cents / 100).absoluteValue
    val remainder = (cents % 100).absoluteValue
    return "$sign$euros.${remainder.toString().padStart(2, '0')}\u00A0€"
}

fun parseEuros(text: String): Long? = try {
    val euros = BigDecimal(text.trim())
    if (euros.stripTrailingZeros().scale() > 2) null
    else euros.movePointRight(2).longValueExact()
} catch (_: NumberFormatException) {
    null
} catch (_: ArithmeticException) {
    null
}
