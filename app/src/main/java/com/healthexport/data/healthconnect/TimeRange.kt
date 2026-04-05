package com.healthexport.data.healthconnect

import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

sealed class TimeRange {

    data object Last24Hours : TimeRange()
    data object LastWeek    : TimeRange()
    data object LastMonth   : TimeRange()

    data class Custom(
        val startDate: LocalDate,
        val endDate: LocalDate,
    ) : TimeRange()

    // ── Display ───────────────────────────────────────────────────────────

    val displayName: String
        get() = when (this) {
            Last24Hours -> "Ultime 24 ore"
            LastWeek    -> "Ultima settimana"
            LastMonth   -> "Ultimo mese"
            is Custom   -> "Personalizzato"
        }

    // ── Conversion to Health Connect filter ───────────────────────────────

    fun toTimeRangeFilter(): TimeRangeFilter {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        return when (this) {
            Last24Hours -> TimeRangeFilter.between(
                now.minus(24, ChronoUnit.HOURS), now
            )
            LastWeek -> TimeRangeFilter.between(
                now.minus(7, ChronoUnit.DAYS), now
            )
            LastMonth -> TimeRangeFilter.between(
                now.minus(30, ChronoUnit.DAYS), now
            )
            is Custom -> TimeRangeFilter.between(
                startDate.atStartOfDay(zone).toInstant(),
                endDate.plusDays(1).atStartOfDay(zone).toInstant(),
            )
        }
    }

    // ── Serialisation for DataStore ───────────────────────────────────────

    fun serialise(): String = when (this) {
        Last24Hours -> "LAST_24H"
        LastWeek    -> "LAST_WEEK"
        LastMonth   -> "LAST_MONTH"
        is Custom   -> "CUSTOM|${startDate}|${endDate}"
    }

    companion object {
        fun deserialise(value: String): TimeRange = when {
            value == "LAST_24H"   -> Last24Hours
            value == "LAST_WEEK"  -> LastWeek
            value == "LAST_MONTH" -> LastMonth
            value.startsWith("CUSTOM|") -> {
                val parts = value.split("|")
                Custom(LocalDate.parse(parts[1]), LocalDate.parse(parts[2]))
            }
            else -> LastWeek
        }
    }
}
