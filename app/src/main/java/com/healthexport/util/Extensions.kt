package com.healthexport.util

import androidx.health.connect.client.records.InstantaneousRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.Record
import java.time.Instant

/** Returns the primary timestamp of a Health Connect record (start time or instant time). */
fun Record.primaryTimestamp(): Instant = when (this) {
    is IntervalRecord      -> startTime
    is InstantaneousRecord -> time
    else                   -> Instant.EPOCH
}
