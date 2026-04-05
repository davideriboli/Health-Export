package com.healthexport.data.healthconnect

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.time.TimeRangeFilter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * Repository that exposes Health Connect capabilities to the ViewModel layer.
 *
 * At this stage (Module 2) it focuses on permission checking and raw record
 * reading. The schema-aware extraction used during export is added in Module 3.
 */
@Singleton
class HealthConnectRepository @Inject constructor(
    private val manager: HealthConnectManager,
) {
    val isAvailable: Boolean get() = manager.isAvailable
    val sdkStatus: Int       get() = manager.sdkStatus

    suspend fun getGrantedPermissions(): Set<String> =
        manager.getGrantedPermissions()

    fun requiredPermissionsFor(types: Collection<HealthRecordType>): Set<String> =
        manager.requiredPermissionsFor(types)

    fun missingPermissions(
        types: Collection<HealthRecordType>,
        granted: Set<String>,
    ): Set<String> = requiredPermissionsFor(types) - granted

    /**
     * Reads all records of [recordClass] for the given [timeRange].
     *
     * Type-safe wrapper used by the export pipeline (Module 3).
     */
    suspend fun <T : Record> readRecords(
        recordClass: KClass<T>,
        timeRange: TimeRange,
    ): List<T> = manager.readRecords(recordClass, timeRange.toTimeRangeFilter())

    /** Convenience overload that accepts a [HealthRecordType]. */
    @Suppress("UNCHECKED_CAST")
    suspend fun readRecordsForType(
        type: HealthRecordType,
        timeRange: TimeRange,
    ): List<Record> = readRecords(type.recordClass as KClass<Record>, timeRange)
}
