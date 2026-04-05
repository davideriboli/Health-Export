package com.healthexport.util

import androidx.health.connect.client.records.Record
import java.time.Instant

/**
 * Returns the primary timestamp of a Health Connect record.
 *
 * InstantaneousRecord and IntervalRecord are internal in health-connect alpha —
 * we use reflection to access the public getStartTime() / getTime() Java methods
 * that all concrete record classes expose.
 */
fun Record.primaryTimestamp(): Instant =
    runCatching { javaClass.getMethod("getStartTime").invoke(this) as Instant }
        .getOrElse {
            runCatching { javaClass.getMethod("getTime").invoke(this) as Instant }
                .getOrDefault(Instant.EPOCH)
        }
