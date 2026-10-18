package io.github.parcival42.workouttracker.data.backup

import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanEntry
import io.github.parcival42.workouttracker.data.PlanType
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.TrainingPlan
import io.github.parcival42.workouttracker.data.UserSettings
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val devices: List<Device>,
    val plans: List<TrainingPlan>,
    val planEntries: List<PlanEntry>,
    val sets: List<SetLog>,
    val cardioSessions: List<CardioLog> = emptyList(),
    val settings: UserSettings = UserSettings(),
)

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BackupJson {
    private const val FORMAT = "workout-tracker-backup"
    // v2: plans carry "isDefault" (absent in v1 files, read as false).
    // v3: devices carry "type", plan entries "targetDurationSeconds", plus "cardioSessions"
    //     (absent in older files, read as strength devices / default duration / no sessions).
    // v4: sets and cardio sessions carry "planId" (nullable), plus "settings"
    //     (absent in older files, read as no plan / default settings).
    // v5: plans carry "type" and the circuit settings, plan entries "stationSeconds", sets a
    //     nullable "durationSeconds" and nullable "reps" (absent in older files, read as normal
    //     plans / no circuit values / untimed sets).
    const val VERSION = 5

    fun encode(data: BackupData, exportedAt: Long): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("exportedAt", exportedAt)
        put("devices", JSONArray(data.devices.map { d ->
            JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("imageUri", d.imageUri ?: JSONObject.NULL)
                put("archived", d.archived)
                // Key name kept from v1 for compatibility; holds the device's minimum weight.
                put("startWeightGrams", d.minWeightGrams)
                put("weightStepGrams", d.weightStepGrams)
                put("type", d.type.name)
            }
        }))
        put("plans", JSONArray(data.plans.map { p ->
            JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("isDefault", p.isDefault)
                put("type", p.type.name)
                put("circuitRounds", p.circuitRounds ?: JSONObject.NULL)
                put("circuitStationSeconds", p.circuitStationSeconds ?: JSONObject.NULL)
                put("circuitRestSeconds", p.circuitRestSeconds ?: JSONObject.NULL)
                put("circuitCountdownSeconds", p.circuitCountdownSeconds ?: JSONObject.NULL)
            }
        }))
        put("planEntries", JSONArray(data.planEntries.map { e ->
            JSONObject().apply {
                put("id", e.id)
                put("planId", e.planId)
                put("deviceId", e.deviceId)
                put("sortOrder", e.sortOrder)
                put("targetSets", e.targetSets)
                put("targetReps", e.targetReps)
                put("targetDurationSeconds", e.targetDurationSeconds)
                put("stationSeconds", e.stationSeconds ?: JSONObject.NULL)
            }
        }))
        put("sets", JSONArray(data.sets.map { s ->
            JSONObject().apply {
                put("id", s.id)
                put("deviceId", s.deviceId)
                put("performedAt", s.performedAt)
                put("weightGrams", s.weightGrams)
                put("reps", s.reps ?: JSONObject.NULL)
                put("planId", s.planId ?: JSONObject.NULL)
                put("durationSeconds", s.durationSeconds ?: JSONObject.NULL)
            }
        }))
        put("cardioSessions", JSONArray(data.cardioSessions.map { c ->
            JSONObject().apply {
                put("id", c.id)
                put("deviceId", c.deviceId)
                put("performedAt", c.performedAt)
                put("durationSeconds", c.durationSeconds)
                put("planId", c.planId ?: JSONObject.NULL)
            }
        }))
        put("settings", JSONObject().apply {
            put("defaultTargetSets", data.settings.defaultTargetSets)
            put("defaultTargetReps", data.settings.defaultTargetReps)
            put("defaultTargetDurationSeconds", data.settings.defaultTargetDurationSeconds)
        })
    }.toString(2)

    /** Parses and fully validates a backup; throws [InvalidBackupException] before anything is imported. */
    fun decode(json: String): BackupData {
        val data = try {
            val root = JSONObject(json)
            if (root.optString("format") != FORMAT) throw InvalidBackupException("Not a Workout Tracker backup")
            val version = root.getInt("version")
            if (version > VERSION) throw InvalidBackupException("Backup version $version is newer than supported")

            BackupData(
                devices = root.getJSONArray("devices").objects().map { o ->
                    Device(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        imageUri = if (o.isNull("imageUri")) null else o.getString("imageUri"),
                        archived = o.getBoolean("archived"),
                        minWeightGrams = o.getInt("startWeightGrams"),
                        weightStepGrams = o.getInt("weightStepGrams"),
                        type = DeviceType.valueOf(o.optString("type", DeviceType.STRENGTH.name)),
                    )
                },
                plans = root.getJSONArray("plans").objects().map { o ->
                    TrainingPlan(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        isDefault = o.optBoolean("isDefault", false),
                        type = PlanType.valueOf(o.optString("type", PlanType.NORMAL.name)),
                        circuitRounds = o.optIntOrNull("circuitRounds"),
                        circuitStationSeconds = o.optIntOrNull("circuitStationSeconds"),
                        circuitRestSeconds = o.optIntOrNull("circuitRestSeconds"),
                        circuitCountdownSeconds = o.optIntOrNull("circuitCountdownSeconds"),
                    )
                },
                planEntries = root.getJSONArray("planEntries").objects().map { o ->
                    PlanEntry(
                        id = o.getLong("id"),
                        planId = o.getLong("planId"),
                        deviceId = o.getLong("deviceId"),
                        sortOrder = o.getInt("sortOrder"),
                        targetSets = o.getInt("targetSets"),
                        targetReps = o.getInt("targetReps"),
                        targetDurationSeconds = o.optInt(
                            "targetDurationSeconds",
                            PlanEntry.DEFAULT_TARGET_DURATION_SECONDS,
                        ),
                        stationSeconds = o.optIntOrNull("stationSeconds"),
                    )
                },
                sets = root.getJSONArray("sets").objects().map { o ->
                    SetLog(
                        id = o.getLong("id"),
                        deviceId = o.getLong("deviceId"),
                        performedAt = o.getLong("performedAt"),
                        weightGrams = o.getInt("weightGrams"),
                        reps = o.optIntOrNull("reps"),
                        planId = o.optPlanId(),
                        durationSeconds = o.optIntOrNull("durationSeconds"),
                    )
                },
                cardioSessions = (root.optJSONArray("cardioSessions") ?: JSONArray()).objects().map { o ->
                    CardioLog(
                        id = o.getLong("id"),
                        deviceId = o.getLong("deviceId"),
                        performedAt = o.getLong("performedAt"),
                        durationSeconds = o.getInt("durationSeconds"),
                        planId = o.optPlanId(),
                    )
                },
                settings = root.optJSONObject("settings")?.let { o ->
                    val defaults = UserSettings()
                    UserSettings(
                        defaultTargetSets = o.optInt("defaultTargetSets", defaults.defaultTargetSets),
                        defaultTargetReps = o.optInt("defaultTargetReps", defaults.defaultTargetReps),
                        defaultTargetDurationSeconds = o.optInt(
                            "defaultTargetDurationSeconds",
                            defaults.defaultTargetDurationSeconds,
                        ),
                    )
                } ?: UserSettings(),
            )
        } catch (e: InvalidBackupException) {
            throw e
        } catch (e: Exception) {
            throw InvalidBackupException("Malformed backup file", e)
        }
        validate(data)
        return data
    }

    private fun validate(data: BackupData) {
        val deviceIds = data.devices.map { it.id }.toSet()
        val strengthDeviceIds = data.devices.filter { it.type == DeviceType.STRENGTH }.map { it.id }.toSet()
        val cardioDeviceIds = data.devices.filter { it.type == DeviceType.CARDIO }.map { it.id }.toSet()
        val planIds = data.plans.map { it.id }.toSet()
        fun check(condition: Boolean, message: String) {
            if (!condition) throw InvalidBackupException(message)
        }
        fun List<Long>.areValidIds() = toSet().size == size && all { it > 0 }
        check(deviceIds.size == data.devices.size && deviceIds.all { it > 0 }, "Invalid device ids")
        check(planIds.size == data.plans.size && planIds.all { it > 0 }, "Invalid plan ids")
        check(data.planEntries.map { it.id }.areValidIds(), "Invalid plan entry ids")
        check(data.sets.map { it.id }.areValidIds(), "Invalid set ids")
        check(data.cardioSessions.map { it.id }.areValidIds(), "Invalid cardio session ids")
        check(data.plans.count { it.isDefault } <= 1, "More than one default plan")
        check(
            data.plans.all { p ->
                (p.circuitRounds ?: 1) > 0 && (p.circuitStationSeconds ?: 1) > 0 &&
                    (p.circuitRestSeconds ?: 0) >= 0 && (p.circuitCountdownSeconds ?: 0) >= 0
            },
            "Invalid circuit settings",
        )
        check(data.devices.all { it.name.isNotBlank() && it.minWeightGrams >= 0 && it.weightStepGrams > 0 }, "Invalid device values")
        check(data.planEntries.all { it.planId in planIds && it.deviceId in deviceIds }, "Plan entry references a missing plan or device")
        check(
            data.planEntries.all {
                it.targetSets > 0 && it.targetReps > 0 && it.targetDurationSeconds > 0 && (it.stationSeconds ?: 1) > 0
            },
            "Invalid plan entry targets",
        )
        check(data.sets.all { it.deviceId in strengthDeviceIds }, "Set references a missing or non-strength device")
        check(
            data.sets.all {
                it.weightGrams >= 0 && (it.reps ?: 1) > 0 && (it.durationSeconds ?: 1) > 0 &&
                    (it.reps != null || it.durationSeconds != null)
            },
            "Invalid set values",
        )
        check(
            data.cardioSessions.all { it.deviceId in cardioDeviceIds },
            "Cardio session references a missing or non-cardio device",
        )
        check(data.cardioSessions.all { it.durationSeconds > 0 }, "Invalid cardio session values")
        check(
            data.sets.all { it.planId == null || it.planId in planIds } &&
                data.cardioSessions.all { it.planId == null || it.planId in planIds },
            "Log references a missing plan",
        )
        check(
            data.settings.let {
                it.defaultTargetSets > 0 && it.defaultTargetReps > 0 && it.defaultTargetDurationSeconds > 0
            },
            "Invalid settings",
        )
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

    private fun JSONObject.optPlanId(): Long? = if (isNull("planId")) null else getLong("planId")

    // Missing and null both read as null.
    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
}
