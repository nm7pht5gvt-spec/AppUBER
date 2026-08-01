package com.tuapp.tripadvisor.data.parser

import com.tuapp.tripadvisor.domain.model.TripOffer

object ScreenTextParser {

    private val DISTANCE_REGEX = Regex("""(\d+[.,]?\d*)\s*km""", RegexOption.IGNORE_CASE)
    private val TIME_MINUTES_REGEX = Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE)
    private val PRICE_REGEX = Regex("""\$\s?(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?)""")

    fun parse(screenText: String): TripOffer? {
        val distance = extractDistance(screenText) ?: return null
        val time = extractTimeMinutes(screenText) ?: return null
        val price = extractPrice(screenText) ?: return null

        return TripOffer(
            distanceKm = distance,
            estimatedTimeMinutes = time,
            offeredPrice = price
        )
    }

    private fun extractDistance(text: String): Double? {
        val match = DISTANCE_REGEX.find(text) ?: return null
        return normalizeNumber(match.groupValues[1])
    }

    private fun extractTimeMinutes(text: String): Double? {
        val match = TIME_MINUTES_REGEX.find(text) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    private fun extractPrice(text: String): Double? {
        val match = PRICE_REGEX.find(text) ?: return null
        return normalizePriceString(match.groupValues[1])
    }

    private fun normalizeNumber(raw: String): Double? {
        return raw.replace(",", ".").toDoubleOrNull()
    }

    private fun normalizePriceString(raw: String): Double? {
        val hasComma = raw.contains(",")
        val hasDot = raw.contains(".")

        val cleaned = when {
            hasComma && hasDot -> raw.replace(".", "").replace(",", ".")
            hasComma -> raw.replace(",", ".")
            hasDot && raw.substringAfterLast(".").length == 3 -> raw.replace(".", "")
            else -> raw
        }
        return cleaned.toDoubleOrNull()
    }
}

