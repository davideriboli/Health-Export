package com.healthexport.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * Wraps [HealthConnectClient] and provides safe, lazy access.
 *
 * Why lazy: [HealthConnectClient.getOrCreate] succeeds only when the HC app is
 * installed. Checking availability eagerly at DI time would crash on devices
 * where HC is absent. Instead we check [sdkStatus] first, then use [client].
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val sdkStatus: Int
        get() = HealthConnectClient.getSdkStatus(context)

    val isAvailable: Boolean
        get() = sdkStatus == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    // ── Permissions ───────────────────────────────────────────────────────

    suspend fun getGrantedPermissions(): Set<String> {
        if (!isAvailable) return emptySet()
        return client.permissionController.getGrantedPermissions()
    }

    fun requiredPermissionsFor(types: Collection<HealthRecordType>): Set<String> =
        types.map { it.readPermission }.toSet()

    // ── Data reading ──────────────────────────────────────────────────────

    /**
     * Reads all records of type [T] in the given [timeRangeFilter].
     *
     * Returns an empty list (rather than throwing) when HC is unavailable.
     * Callers should check [isAvailable] before presenting data to the user.
     */
    suspend fun <T : Record> readRecords(
        recordClass: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
    ): List<T> {
        if (!isAvailable) return emptyList()
        val request = ReadRecordsRequest(
            recordType      = recordClass,
            timeRangeFilter = timeRangeFilter,
        )
        return client.readRecords(request).records
    }
}
