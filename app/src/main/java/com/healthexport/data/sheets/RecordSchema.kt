package com.healthexport.data.sheets

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import com.healthexport.data.healthconnect.HealthRecordType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val SOURCE_APP = "HealthExport"

private val formatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private fun Instant.fmt(): String = formatter.format(this)
private fun Double?.orEmpty(): Any = this ?: ""
private fun Long?.orEmpty(): Any   = this ?: ""
private fun Int?.orEmpty(): Any    = this ?: ""

/**
 * Maps a [HealthRecordType] to its Google Sheets column schema and row extraction logic.
 *
 * [columns]     — ordered list of column header names (written once as the first row).
 * [extractRows] — converts a single [Record] into one or more data rows. Record types
 *                 that contain sample lists (HeartRate, Speed, Power) produce one row
 *                 per sample. All others produce a single row.
 */
data class RecordSchema(
    val columns: List<String>,
    val extractRows: (Record) -> List<List<Any?>>,
) {
    companion object {
        private val schemas: Map<HealthRecordType, RecordSchema> by lazy { buildSchemas() }

        fun forType(type: HealthRecordType): RecordSchema =
            schemas[type] ?: error("No schema defined for ${type.id}")

        @Suppress("LongMethod")
        private fun buildSchemas(): Map<HealthRecordType, RecordSchema> = mapOf(

            // ── Activity ──────────────────────────────────────────────────

            HealthRecordType.Steps to RecordSchema(
                columns = listOf("start_time", "end_time", "count", "source_app"),
                extractRows = { r ->
                    r as StepsRecord
                    listOf(listOf(r.startTime.fmt(), r.endTime.fmt(), r.count, SOURCE_APP))
                },
            ),

            HealthRecordType.Distance to RecordSchema(
                columns = listOf("start_time", "end_time", "distance_m", "source_app"),
                extractRows = { r ->
                    r as DistanceRecord
                    listOf(listOf(r.startTime.fmt(), r.endTime.fmt(),
                        r.distance.inMeters, SOURCE_APP))
                },
            ),

            HealthRecordType.ActiveCalories to RecordSchema(
                columns = listOf("start_time", "end_time", "kcal", "source_app"),
                extractRows = { r ->
                    r as ActiveCaloriesBurnedRecord
                    listOf(listOf(r.startTime.fmt(), r.endTime.fmt(),
                        r.energy.inKilocalories, SOURCE_APP))
                },
            ),

            HealthRecordType.TotalCalories to RecordSchema(
                columns = listOf("start_time", "end_time", "kcal", "source_app"),
                extractRows = { r ->
                    r as TotalCaloriesBurnedRecord
                    listOf(listOf(r.startTime.fmt(), r.endTime.fmt(),
                        r.energy.inKilocalories, SOURCE_APP))
                },
            ),

            HealthRecordType.ExerciseSession to RecordSchema(
                columns = listOf("start_time", "end_time", "exercise_type",
                    "duration_min", "title", "source_app"),
                extractRows = { r ->
                    r as ExerciseSessionRecord
                    listOf(listOf(
                        r.startTime.fmt(),
                        r.endTime.fmt(),
                        r.exerciseType,
                        ChronoUnit.MINUTES.between(r.startTime, r.endTime),
                        r.title ?: "",
                        SOURCE_APP,
                    ))
                },
            ),

            HealthRecordType.FloorsClimbed to RecordSchema(
                columns = listOf("start_time", "end_time", "floors", "source_app"),
                extractRows = { r ->
                    r as FloorsClimbedRecord
                    listOf(listOf(r.startTime.fmt(), r.endTime.fmt(), r.floors, SOURCE_APP))
                },
            ),

            HealthRecordType.ElevationGained to RecordSchema(
                columns = listOf("start_time", "end_time", "elevation_m", "source_app"),
                extractRows = { r ->
                    r as ElevationGainedRecord
                    listOf(listOf(r.startTime.fmt(), r.endTime.fmt(),
                        r.elevation.inMeters, SOURCE_APP))
                },
            ),

            // Speed: one row per sample
            HealthRecordType.Speed to RecordSchema(
                columns = listOf("session_start", "sample_time", "speed_m_per_s", "source_app"),
                extractRows = { r ->
                    r as SpeedRecord
                    r.samples.map { s ->
                        listOf(r.startTime.fmt(), s.time.fmt(), s.speed.inMetersPerSecond, SOURCE_APP)
                    }
                },
            ),

            // Power: one row per sample
            HealthRecordType.Power to RecordSchema(
                columns = listOf("session_start", "sample_time", "power_w", "source_app"),
                extractRows = { r ->
                    r as PowerRecord
                    r.samples.map { s ->
                        listOf(r.startTime.fmt(), s.time.fmt(), s.power.inWatts, SOURCE_APP)
                    }
                },
            ),

            // ── Body ──────────────────────────────────────────────────────

            HealthRecordType.Weight to RecordSchema(
                columns = listOf("time", "kg", "source_app"),
                extractRows = { r ->
                    r as WeightRecord
                    listOf(listOf(r.time.fmt(), r.weight.inKilograms, SOURCE_APP))
                },
            ),

            HealthRecordType.Height to RecordSchema(
                columns = listOf("time", "m", "source_app"),
                extractRows = { r ->
                    r as HeightRecord
                    listOf(listOf(r.time.fmt(), r.height.inMeters, SOURCE_APP))
                },
            ),

            HealthRecordType.BodyFat to RecordSchema(
                columns = listOf("time", "percentage", "source_app"),
                extractRows = { r ->
                    r as BodyFatRecord
                    listOf(listOf(r.time.fmt(), r.percentage.value, SOURCE_APP))
                },
            ),

            HealthRecordType.BoneMass to RecordSchema(
                columns = listOf("time", "kg", "source_app"),
                extractRows = { r ->
                    r as BoneMassRecord
                    listOf(listOf(r.time.fmt(), r.mass.inKilograms, SOURCE_APP))
                },
            ),

            HealthRecordType.LeanBodyMass to RecordSchema(
                columns = listOf("time", "kg", "source_app"),
                extractRows = { r ->
                    r as LeanBodyMassRecord
                    listOf(listOf(r.time.fmt(), r.mass.inKilograms, SOURCE_APP))
                },
            ),

            HealthRecordType.BasalMetabolicRate to RecordSchema(
                columns = listOf("time", "kcal_per_day", "source_app"),
                extractRows = { r ->
                    r as BasalMetabolicRateRecord
                    listOf(listOf(r.time.fmt(), r.basalMetabolicRate.inKilocaloriesPerDay, SOURCE_APP))
                },
            ),

            // ── Vitals ────────────────────────────────────────────────────

            // HeartRate: one row per sample
            HealthRecordType.HeartRate to RecordSchema(
                columns = listOf("session_start", "sample_time", "bpm", "source_app"),
                extractRows = { r ->
                    r as HeartRateRecord
                    r.samples.map { s ->
                        listOf(r.startTime.fmt(), s.time.fmt(), s.beatsPerMinute, SOURCE_APP)
                    }
                },
            ),

            HealthRecordType.RestingHeartRate to RecordSchema(
                columns = listOf("time", "bpm", "source_app"),
                extractRows = { r ->
                    r as RestingHeartRateRecord
                    listOf(listOf(r.time.fmt(), r.beatsPerMinute, SOURCE_APP))
                },
            ),

            HealthRecordType.HeartRateVariability to RecordSchema(
                columns = listOf("time", "rmssd_ms", "source_app"),
                extractRows = { r ->
                    r as HeartRateVariabilityRmssdRecord
                    listOf(listOf(r.time.fmt(), r.heartRateVariabilityMillis, SOURCE_APP))
                },
            ),

            HealthRecordType.BloodPressure to RecordSchema(
                columns = listOf("time", "systolic_mmhg", "diastolic_mmhg",
                    "body_position", "measurement_location", "source_app"),
                extractRows = { r ->
                    r as BloodPressureRecord
                    listOf(listOf(
                        r.time.fmt(),
                        r.systolic.inMillimetersOfMercury,
                        r.diastolic.inMillimetersOfMercury,
                        r.bodyPosition,
                        r.measurementLocation,
                        SOURCE_APP,
                    ))
                },
            ),

            HealthRecordType.BloodGlucose to RecordSchema(
                columns = listOf("time", "mmol_per_l", "relation_to_meal",
                    "specimen_source", "source_app"),
                extractRows = { r ->
                    r as BloodGlucoseRecord
                    listOf(listOf(
                        r.time.fmt(),
                        r.level.inMillimolesPerLiter,
                        r.relationToMeal,
                        r.specimenSource,
                        SOURCE_APP,
                    ))
                },
            ),

            HealthRecordType.OxygenSaturation to RecordSchema(
                columns = listOf("time", "percentage", "source_app"),
                extractRows = { r ->
                    r as OxygenSaturationRecord
                    listOf(listOf(r.time.fmt(), r.percentage.value, SOURCE_APP))
                },
            ),

            HealthRecordType.RespiratoryRate to RecordSchema(
                columns = listOf("time", "rpm", "source_app"),
                extractRows = { r ->
                    r as RespiratoryRateRecord
                    listOf(listOf(r.time.fmt(), r.rate, SOURCE_APP))
                },
            ),

            HealthRecordType.BodyTemperature to RecordSchema(
                columns = listOf("time", "celsius", "measurement_location", "source_app"),
                extractRows = { r ->
                    r as BodyTemperatureRecord
                    listOf(listOf(
                        r.time.fmt(),
                        r.temperature.inCelsius,
                        r.measurementLocation,
                        SOURCE_APP,
                    ))
                },
            ),

            // ── Sleep ─────────────────────────────────────────────────────

            // One row for the session summary + one row per sleep stage
            HealthRecordType.SleepSession to RecordSchema(
                columns = listOf("start_time", "end_time", "duration_min",
                    "stage_type", "source_app"),
                extractRows = { r ->
                    r as SleepSessionRecord
                    val sessionRow = listOf(
                        r.startTime.fmt(), r.endTime.fmt(),
                        ChronoUnit.MINUTES.between(r.startTime, r.endTime),
                        "session",
                        SOURCE_APP,
                    )
                    val stageRows = r.stages.map { stage ->
                        listOf(
                            stage.startTime.fmt(), stage.endTime.fmt(),
                            ChronoUnit.MINUTES.between(stage.startTime, stage.endTime),
                            stage.stage.toString(),
                            SOURCE_APP,
                        )
                    }
                    listOf(sessionRow) + stageRows
                },
            ),

            // ── Nutrition ─────────────────────────────────────────────────

            HealthRecordType.Nutrition to RecordSchema(
                columns = listOf("start_time", "end_time", "name",
                    "energy_kcal", "carbs_g", "protein_g", "fat_g",
                    "fiber_g", "sugar_g", "sodium_mg", "source_app"),
                extractRows = { r ->
                    r as NutritionRecord
                    listOf(listOf(
                        r.startTime.fmt(), r.endTime.fmt(),
                        r.name ?: "",
                        r.energy?.inKilocalories.orEmpty(),
                        r.totalCarbohydrate?.inGrams.orEmpty(),
                        r.protein?.inGrams.orEmpty(),
                        r.totalFat?.inGrams.orEmpty(),
                        r.dietaryFiber?.inGrams.orEmpty(),
                        r.sugar?.inGrams.orEmpty(),
                        r.sodium?.inGrams?.let { it * 1000 }.orEmpty(), // g → mg
                        SOURCE_APP,
                    ))
                },
            ),

            HealthRecordType.Hydration to RecordSchema(
                columns = listOf("start_time", "end_time", "volume_l", "source_app"),
                extractRows = { r ->
                    r as HydrationRecord
                    listOf(listOf(r.startTime.fmt(), r.endTime.fmt(),
                        r.volume.inLiters, SOURCE_APP))
                },
            ),

            // ── Reproductive ──────────────────────────────────────────────

            HealthRecordType.MenstruationFlow to RecordSchema(
                columns = listOf("time", "flow", "is_abnormal", "source_app"),
                extractRows = { r ->
                    r as MenstruationFlowRecord
                    listOf(listOf(r.time.fmt(), r.flow, r.isAbnormal, SOURCE_APP))
                },
            ),

            HealthRecordType.CervicalMucus to RecordSchema(
                columns = listOf("time", "appearance", "sensation", "source_app"),
                extractRows = { r ->
                    r as CervicalMucusRecord
                    listOf(listOf(r.time.fmt(), r.appearance, r.sensation, SOURCE_APP))
                },
            ),

            HealthRecordType.OvulationTest to RecordSchema(
                columns = listOf("time", "result", "source_app"),
                extractRows = { r ->
                    r as OvulationTestRecord
                    listOf(listOf(r.time.fmt(), r.result, SOURCE_APP))
                },
            ),

            HealthRecordType.SexualActivity to RecordSchema(
                columns = listOf("time", "protection_used", "source_app"),
                extractRows = { r ->
                    r as SexualActivityRecord
                    listOf(listOf(r.time.fmt(), r.protectionUsed, SOURCE_APP))
                },
            ),

            HealthRecordType.IntermenstrualBleeding to RecordSchema(
                columns = listOf("time", "source_app"),
                extractRows = { r ->
                    r as IntermenstrualBleedingRecord
                    listOf(listOf(r.time.fmt(), SOURCE_APP))
                },
            ),
        )
    }
}
