package com.example.workouttracker.data.backup

import com.example.workouttracker.data.Device
import com.example.workouttracker.data.PlanEntry
import com.example.workouttracker.data.SetLog
import com.example.workouttracker.data.TrainingPlan
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val devices: List<Device>,
    val plans: List<TrainingPlan>,
    val planEntries: List<PlanEntry>,
    val sets: List<SetLog>,
)

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BackupJson {
    private const val FORMAT = "workout-tracker-backup"
    // v2: plans carry "isDefault" (absent in v1 files, read as false).
    const val VERSION = 2

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
                put("startWeightGrams", d.startWeightGrams)
                put("weightStepGrams", d.weightStepGrams)
            }
        }))
        put("plans", JSONArray(data.plans.map { p ->
            JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("isDefault", p.isDefault)
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
            }
        }))
        put("sets", JSONArray(data.sets.map { s ->
            JSONObject().apply {
                put("id", s.id)
                put("deviceId", s.deviceId)
                put("performedAt", s.performedAt)
                put("weightGrams", s.weightGrams)
                put("reps", s.reps)
            }
        }))
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
                        startWeightGrams = o.getInt("startWeightGrams"),
                        weightStepGrams = o.getInt("weightStepGrams"),
                    )
                },
                plans = root.getJSONArray("plans").objects().map { o ->
                    TrainingPlan(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        isDefault = o.optBoolean("isDefault", false),
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
                    )
                },
                sets = root.getJSONArray("sets").objects().map { o ->
                    SetLog(
                        id = o.getLong("id"),
                        deviceId = o.getLong("deviceId"),
                        performedAt = o.getLong("performedAt"),
                        weightGrams = o.getInt("weightGrams"),
                        reps = o.getInt("reps"),
                    )
                },
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
        val planIds = data.plans.map { it.id }.toSet()
        fun check(condition: Boolean, message: String) {
            if (!condition) throw InvalidBackupException(message)
        }
        check(deviceIds.size == data.devices.size && deviceIds.all { it > 0 }, "Invalid device ids")
        check(planIds.size == data.plans.size && planIds.all { it > 0 }, "Invalid plan ids")
        check(data.planEntries.map { it.id }.let { ids -> ids.toSet().size == ids.size && ids.all { it > 0 } }, "Invalid plan entry ids")
        check(data.sets.map { it.id }.let { ids -> ids.toSet().size == ids.size && ids.all { it > 0 } }, "Invalid set ids")
        check(data.plans.count { it.isDefault } <= 1, "More than one default plan")
        check(data.devices.all { it.name.isNotBlank() && it.startWeightGrams >= 0 && it.weightStepGrams > 0 }, "Invalid device values")
        check(data.planEntries.all { it.planId in planIds && it.deviceId in deviceIds }, "Plan entry references a missing plan or device")
        check(data.planEntries.all { it.targetSets > 0 && it.targetReps > 0 }, "Invalid plan entry targets")
        check(data.sets.all { it.deviceId in deviceIds }, "Set references a missing device")
        check(data.sets.all { it.weightGrams >= 0 && it.reps > 0 }, "Invalid set values")
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
}
