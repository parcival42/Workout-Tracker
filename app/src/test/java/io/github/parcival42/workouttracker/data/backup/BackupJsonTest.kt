package io.github.parcival42.workouttracker.data.backup

import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanEntry
import io.github.parcival42.workouttracker.data.PlanType
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.TrainingPlan
import io.github.parcival42.workouttracker.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupJsonTest {

    private val sample = BackupData(
        devices = listOf(
            Device(id = 1, name = "Leg Press", minWeightGrams = 40_000, weightStepGrams = 5_000),
            Device(id = 2, name = "Old Rowing", imageUri = "content://image", archived = true),
            Device(id = 3, name = "Treadmill", type = DeviceType.CARDIO, tracksDistance = true),
            Device(id = 4, name = "Plank", type = DeviceType.BODYWEIGHT, measure = BodyweightMeasure.TIME),
        ),
        plans = listOf(
            TrainingPlan(id = 7, name = "Upper Body", isDefault = true),
            TrainingPlan(
                id = 8, name = "Legs", type = PlanType.CIRCUIT, circuitRounds = 2, circuitStationSeconds = 45,
                circuitRestSeconds = 0, circuitCountdownSeconds = 5,
            ),
        ),
        planEntries = listOf(
            PlanEntry(id = 3, planId = 7, deviceId = 1, sortOrder = 0, targetSets = 3, targetReps = 12, weightGrams = 22_500),
            PlanEntry(
                id = 4, planId = 7, deviceId = 3, sortOrder = 1, targetSets = 3, targetReps = 10,
                targetDurationSeconds = 1_800,
            ),
            PlanEntry(
                id = 5, planId = 8, deviceId = 1, sortOrder = 0, targetSets = 3, targetReps = 15, stationSeconds = 60,
            ),
        ),
        sets = listOf(
            SetLog(id = 10, deviceId = 1, performedAt = 1_760_000_000_000, weightGrams = 20_000, reps = 10, planId = 7),
            SetLog(id = 11, deviceId = 2, performedAt = 1_760_000_000_001, weightGrams = 22_500, reps = 10),
            SetLog(id = 13, deviceId = 4, performedAt = 1_760_000_000_004, weightGrams = 0, reps = null, durationSeconds = 45),
            SetLog(
                id = 12, deviceId = 1, performedAt = 1_760_000_000_003, weightGrams = 10_000, reps = null, planId = 8,
                durationSeconds = 120,
            ),
        ),
        cardioSessions = listOf(
            CardioLog(id = 20, deviceId = 3, performedAt = 1_760_000_000_002, durationSeconds = 1_500, planId = 8),
            CardioLog(
                id = 21, deviceId = 3, performedAt = 1_760_000_000_005, durationSeconds = 1_800, planId = 7,
                distanceMeters = 2_400,
            ),
        ),
        settings = UserSettings(defaultTargetSets = 4, defaultTargetReps = 8, defaultTargetDurationSeconds = 900),
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
        val plan = BackupJson.decode(v1).plans.single()
        assertEquals(false, plan.isDefault)
        assertEquals(PlanType.NORMAL, plan.type)
        assertNull(plan.circuitRounds)
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

    @Test
    fun readsVersion3WithoutPlansOnLogsAndDefaultSettings() {
        val v3 = """
            {"format":"workout-tracker-backup","version":3,"exportedAt":0,
             "devices":[{"id":1,"name":"Leg Press","imageUri":null,"archived":false,"startWeightGrams":0,"weightStepGrams":2500,"type":"STRENGTH"}],
             "plans":[{"id":7,"name":"Upper Body","isDefault":false}],
             "planEntries":[],
             "sets":[{"id":10,"deviceId":1,"performedAt":0,"weightGrams":20000,"reps":10}],
             "cardioSessions":[]}
        """.trimIndent()
        val data = BackupJson.decode(v3)
        assertNull(data.sets.single().planId)
        assertEquals(UserSettings(), data.settings)
    }

    @Test
    fun readsVersion6WithoutDistance() {
        val v6 = """
            {"format":"workout-tracker-backup","version":6,"exportedAt":0,
             "devices":[{"id":3,"name":"Treadmill","imageUri":null,"archived":false,"startWeightGrams":0,"weightStepGrams":2500,"type":"CARDIO","measure":"REPS"}],
             "plans":[],
             "planEntries":[],
             "sets":[],
             "cardioSessions":[{"id":20,"deviceId":3,"performedAt":0,"durationSeconds":1800,"planId":null}]}
        """.trimIndent()
        val data = BackupJson.decode(v6)
        assertEquals(false, data.devices.single().tracksDistance)
        assertNull(data.cardioSessions.single().distanceMeters)
    }

    @Test
    fun currentVersionNeedsNoDerivedEntryValues() {
        val data = BackupJson.decode(BackupJson.encode(sample, 0))
        assertEquals(false, data.needsPlanEntryValues)
        assertEquals(22_500, data.planEntries.first { it.id == 3L }.weightGrams)
    }

    @Test
    fun readsVersion7WithoutEntryWeightAndMarksItForDerivedValues() {
        val v7 = """
            {"format":"workout-tracker-backup","version":7,"exportedAt":0,
             "devices":[{"id":1,"name":"Leg Press","imageUri":null,"archived":false,"startWeightGrams":40000,"weightStepGrams":5000,"type":"STRENGTH","measure":"REPS","tracksDistance":false}],
             "plans":[{"id":7,"name":"Upper Body","isDefault":false}],
             "planEntries":[{"id":3,"planId":7,"deviceId":1,"sortOrder":0,"targetSets":3,"targetReps":10,"targetDurationSeconds":1200,"stationSeconds":null}],
             "sets":[],
             "cardioSessions":[]}
        """.trimIndent()
        val data = BackupJson.decode(v7)
        assertEquals(0, data.planEntries.single().weightGrams)
        assertEquals(true, data.needsPlanEntryValues)
    }

    @Test
    fun rejectsNegativeEntryWeight() {
        val broken = sample.copy(planEntries = sample.planEntries.map { it.copy(weightGrams = -1) })
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(BackupJson.encode(broken, 0)) }
    }

    @Test
    fun rejectsInvalidDistance() {
        val broken = sample.copy(cardioSessions = sample.cardioSessions.map { it.copy(distanceMeters = 0) })
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(BackupJson.encode(broken, 0)) }
    }

    @Test
    fun rejectsLogsOfMissingPlan() {
        val broken = sample.copy(sets = sample.sets.map { it.copy(planId = 99) })
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(BackupJson.encode(broken, 0)) }
    }

    @Test
    fun rejectsInvalidSettings() {
        val broken = sample.copy(settings = UserSettings(defaultTargetSets = 0))
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode(BackupJson.encode(broken, 0)) }
    }
}
