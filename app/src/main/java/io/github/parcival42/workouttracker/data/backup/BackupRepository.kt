package io.github.parcival42.workouttracker.data.backup

import androidx.room.withTransaction
import io.github.parcival42.workouttracker.data.AppDatabase

class BackupRepository(private val database: AppDatabase) {

    suspend fun exportJson(): String {
        val data = database.withTransaction {
            BackupData(
                devices = database.deviceDao().getAll(),
                plans = database.planDao().getAllPlans(),
                planEntries = database.planDao().getAllEntries(),
                sets = database.setLogDao().getAll(),
                cardioSessions = database.cardioLogDao().getAll(),
            )
        }
        return BackupJson.encode(data, exportedAt = System.currentTimeMillis())
    }

    /** Replaces all data with the backup. The file is validated before anything is deleted. */
    suspend fun importJson(json: String) {
        val data = BackupJson.decode(json)
        database.withTransaction {
            database.setLogDao().deleteAll()
            database.cardioLogDao().deleteAll()
            database.planDao().deleteAllEntries()
            database.planDao().deleteAllPlans()
            database.deviceDao().deleteAll()

            database.deviceDao().insertAll(data.devices)
            database.planDao().insertPlans(data.plans)
            database.planDao().insertEntries(data.planEntries)
            database.setLogDao().insertAll(data.sets)
            database.cardioLogDao().insertAll(data.cardioSessions)
        }
    }
}
