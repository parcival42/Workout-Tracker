package com.example.workouttracker.data.backup

import com.example.workouttracker.data.CardioLog
import com.example.workouttracker.data.Device
import com.example.workouttracker.data.DeviceType
import com.example.workouttracker.data.PlanEntry
import com.example.workouttracker.data.SetLog
import com.example.workouttracker.data.TrainingPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupJsonTest {

    private val sample = BackupData(
        devices = listOf(
            Device(id = 1, name = "Leg Press", minWeightGrams = 40_000, weightStepGrams = 5_000),
            Device(id = 2, name = "Old Rowing", imageUri = "content://image", archived = true),
            Device(id = 3, name = "Treadmill", type = DeviceType.CARDIO),
        ),
        plans = listOf(TrainingPlan(id = 7, name = "Upper Body", isDefault = true), TrainingPlan(id = 8, name = "Legs")),
        planEntries = listOf(
            PlanEntry(id = 3, planId = 7, deviceId = 1, sortOrder = 0, targetSets = 3, targetReps = 12),
            PlanEntry(
                id = 4, planId = 7, deviceId = 3, sortOrder = 1, targetSets = 3, targetReps = 10,
                targetDurationSeconds = 1_800,
            ),
        ),
        sets = listOf(
            SetLog(id = 10, deviceId = 1, performedAt = 1_760_000_000_000, weightGrams = 20_000, reps = 10),
            SetLog(id = 11, deviceId = 2, performedAt = 1_760_000_000_001, weightGrams = 22_500, reps = 10),
        ),
        cardioSessions = listOf(
            CardioLog(id = 20, deviceId = 3, performedAt = 1_760_000_000_002, durationSeconds = 1_500),
        ),
    )

    @Test
    fun roundTripKeepsAllData() {
        assertEquals(sample, BackupJson.decode(BackupJson.encode(sample, exportedAt = 0)))
    }

    @Test
    fun rejectsForeignJson() {
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode("""{"hello":"world"}""") }
    }

    @Test
    fun rejectsMalformedJson() {
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode("not json") }
    }

    @Test
    fun rejectsNewerVersion() {
        val json = BackupJson.encode(sample, 0)
            .replace("\"version\": ${BackupJson.VERSION}", "\"version\": ${BackupJson.VERSION + 1}")
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(json) }
    }

    @Test
    fun readsVersion1WithoutDefaultFlag() {
        val v1 = """
            {"format":"workout-tracker-backup","version":1,"exportedAt":0,
             "devices":[{"id":1,"name":"Leg Press","imageUri":null,"archived":false,"startWeightGrams":0,"weightStepGrams":2500}],
             "plans":[{"id":7,"name":"Upper Body"}],
             "planEntries":[],"sets":[]}
        """.trimIndent()
        assertEquals(false, BackupJson.decode(v1).plans.single().isDefault)
    }

    @Test
    fun readsVersion2AsStrengthOnly() {
        val v2 = """
            {"format":"workout-tracker-backup","version":2,"exportedAt":0,
             "devices":[{"id":1,"name":"Leg Press","imageUri":null,"archived":false,"startWeightGrams":0,"weightStepGrams":2500}],
             "plans":[{"id":7,"name":"Upper Body","isDefault":false}],
             "planEntries":[{"id":3,"planId":7,"deviceId":1,"sortOrder":0,"targetSets":3,"targetReps":10}],
             "sets":[]}
        """.trimIndent()
        val data = BackupJson.decode(v2)
        assertEquals(DeviceType.STRENGTH, data.devices.single().type)
        assertEquals(PlanEntry.DEFAULT_TARGET_DURATION_SECONDS, data.planEntries.single().targetDurationSeconds)
        assertEquals(emptyList<CardioLog>(), data.cardioSessions)
    }

    @Test
    fun rejectsLogsOnDeviceOfWrongType() {
        val setOnCardio = sample.copy(
            sets = sample.sets + SetLog(id = 12, deviceId = 3, performedAt = 0, weightGrams = 0, reps = 1),
        )
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(BackupJson.encode(setOnCardio, 0)) }
        val sessionOnStrength = sample.copy(
            cardioSessions = sample.cardioSessions + CardioLog(id = 21, deviceId = 1, performedAt = 0, durationSeconds = 60),
        )
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(BackupJson.encode(sessionOnStrength, 0)) }
    }

    @Test
    fun rejectsTwoDefaultPlans() {
        val broken = sample.copy(plans = sample.plans.map { it.copy(isDefault = true) })
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(BackupJson.encode(broken, 0)) }
    }

    @Test
    fun rejectsDanglingReferences() {
        val broken = sample.copy(sets = sample.sets + SetLog(id = 12, deviceId = 99, performedAt = 0, weightGrams = 0, reps = 1))
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(BackupJson.encode(broken, 0)) }
    }
}
