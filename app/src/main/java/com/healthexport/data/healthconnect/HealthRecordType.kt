package com.healthexport.data.healthconnect

import androidx.health.connect.client.permission.HealthPermission
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
import com.healthexport.data.healthconnect.RecordCategory.ACTIVITY
import com.healthexport.data.healthconnect.RecordCategory.BODY
import com.healthexport.data.healthconnect.RecordCategory.NUTRITION
import com.healthexport.data.healthconnect.RecordCategory.REPRODUCTIVE
import com.healthexport.data.healthconnect.RecordCategory.SLEEP
import com.healthexport.data.healthconnect.RecordCategory.VITALS
import kotlin.reflect.KClass

/**
 * Represents a single Health Connect record type that the app can export.
 *
 * [id]           — stable identifier used in DataStore (never change).
 * [displayName]  — label shown in the UI.
 * [category]     — used for grouping in Step 1.
 * [sheetTabName] — name of the Google Sheets tab where this type is exported.
 * [recordClass]  — the HC record class; used to build the read request and derive the permission.
 */
data class HealthRecordType(
    val id: String,
    val displayName: String,
    val category: RecordCategory,
    val sheetTabName: String,
    val recordClass: KClass<out Record>,
) {
    val readPermission: String
        get() = HealthPermission.getReadPermission(recordClass)

    companion object {

        // ── Attività ──────────────────────────────────────────────────────
        val Steps = HealthRecordType(
            "steps", "Passi", ACTIVITY, "Steps", StepsRecord::class)
        val Distance = HealthRecordType(
            "distance", "Distanza", ACTIVITY, "Distance", DistanceRecord::class)
        val ActiveCalories = HealthRecordType(
            "active_calories", "Calorie attive", ACTIVITY, "ActiveCalories", ActiveCaloriesBurnedRecord::class)
        val TotalCalories = HealthRecordType(
            "total_calories", "Calorie totali", ACTIVITY, "TotalCalories", TotalCaloriesBurnedRecord::class)
        val ExerciseSession = HealthRecordType(
            "exercise_session", "Sessione di esercizio", ACTIVITY, "ExerciseSessions", ExerciseSessionRecord::class)
        val FloorsClimbed = HealthRecordType(
            "floors_climbed", "Piani saliti", ACTIVITY, "FloorsClimbed", FloorsClimbedRecord::class)
        val ElevationGained = HealthRecordType(
            "elevation_gained", "Dislivello", ACTIVITY, "ElevationGained", ElevationGainedRecord::class)
        val Speed = HealthRecordType(
            "speed", "Velocità", ACTIVITY, "Speed", SpeedRecord::class)
        val Power = HealthRecordType(
            "power", "Potenza", ACTIVITY, "Power", PowerRecord::class)

        // ── Corpo ─────────────────────────────────────────────────────────
        val Weight = HealthRecordType(
            "weight", "Peso", BODY, "Weight", WeightRecord::class)
        val Height = HealthRecordType(
            "height", "Altezza", BODY, "Height", HeightRecord::class)
        val BodyFat = HealthRecordType(
            "body_fat", "Grasso corporeo", BODY, "BodyFat", BodyFatRecord::class)
        val BoneMass = HealthRecordType(
            "bone_mass", "Massa ossea", BODY, "BoneMass", BoneMassRecord::class)
        val LeanBodyMass = HealthRecordType(
            "lean_body_mass", "Massa magra", BODY, "LeanBodyMass", LeanBodyMassRecord::class)
        val BasalMetabolicRate = HealthRecordType(
            "basal_metabolic_rate", "Metabolismo basale", BODY, "BasalMetabolicRate", BasalMetabolicRateRecord::class)

        // ── Segni vitali ──────────────────────────────────────────────────
        val HeartRate = HealthRecordType(
            "heart_rate", "Frequenza cardiaca", VITALS, "HeartRate", HeartRateRecord::class)
        val RestingHeartRate = HealthRecordType(
            "resting_heart_rate", "FC a riposo", VITALS, "RestingHeartRate", RestingHeartRateRecord::class)
        val HeartRateVariability = HealthRecordType(
            "hrv", "Variabilità FC (HRV)", VITALS, "HeartRateVariability", HeartRateVariabilityRmssdRecord::class)
        val BloodPressure = HealthRecordType(
            "blood_pressure", "Pressione sanguigna", VITALS, "BloodPressure", BloodPressureRecord::class)
        val BloodGlucose = HealthRecordType(
            "blood_glucose", "Glicemia", VITALS, "BloodGlucose", BloodGlucoseRecord::class)
        val OxygenSaturation = HealthRecordType(
            "oxygen_saturation", "Saturazione O₂", VITALS, "OxygenSaturation", OxygenSaturationRecord::class)
        val RespiratoryRate = HealthRecordType(
            "respiratory_rate", "Frequenza respiratoria", VITALS, "RespiratoryRate", RespiratoryRateRecord::class)
        val BodyTemperature = HealthRecordType(
            "body_temperature", "Temperatura corporea", VITALS, "BodyTemperature", BodyTemperatureRecord::class)

        // ── Sonno ─────────────────────────────────────────────────────────
        val SleepSession = HealthRecordType(
            "sleep_session", "Sessione di sonno", SLEEP, "Sleep", SleepSessionRecord::class)

        // ── Nutrizione ────────────────────────────────────────────────────
        val Nutrition = HealthRecordType(
            "nutrition", "Nutrizione", NUTRITION, "Nutrition", NutritionRecord::class)
        val Hydration = HealthRecordType(
            "hydration", "Idratazione", NUTRITION, "Hydration", HydrationRecord::class)

        // ── Riproduzione ──────────────────────────────────────────────────
        val MenstruationFlow = HealthRecordType(
            "menstruation_flow", "Flusso mestruale", REPRODUCTIVE, "MenstruationFlow", MenstruationFlowRecord::class)
        val CervicalMucus = HealthRecordType(
            "cervical_mucus", "Muco cervicale", REPRODUCTIVE, "CervicalMucus", CervicalMucusRecord::class)
        val OvulationTest = HealthRecordType(
            "ovulation_test", "Test di ovulazione", REPRODUCTIVE, "OvulationTest", OvulationTestRecord::class)
        val SexualActivity = HealthRecordType(
            "sexual_activity", "Attività sessuale", REPRODUCTIVE, "SexualActivity", SexualActivityRecord::class)
        val IntermenstrualBleeding = HealthRecordType(
            "intermenstrual_bleeding", "Sanguinamento intermestruale", REPRODUCTIVE,
            "IntermenstrualBleeding", IntermenstrualBleedingRecord::class)

        // ── Master list ───────────────────────────────────────────────────
        val all: List<HealthRecordType> = listOf(
            Steps, Distance, ActiveCalories, TotalCalories, ExerciseSession,
            FloorsClimbed, ElevationGained, Speed, Power,
            Weight, Height, BodyFat, BoneMass, LeanBodyMass, BasalMetabolicRate,
            HeartRate, RestingHeartRate, HeartRateVariability, BloodPressure,
            BloodGlucose, OxygenSaturation, RespiratoryRate, BodyTemperature,
            SleepSession,
            Nutrition, Hydration,
            MenstruationFlow, CervicalMucus, OvulationTest, SexualActivity, IntermenstrualBleeding,
        )

        val byCategory: Map<RecordCategory, List<HealthRecordType>> =
            all.groupBy { it.category }

        fun byId(id: String): HealthRecordType? = all.firstOrNull { it.id == id }
    }
}
