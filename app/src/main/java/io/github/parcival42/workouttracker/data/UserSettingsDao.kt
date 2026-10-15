package io.github.parcival42.workouttracker.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = ${UserSettings.ID}")
    fun observe(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = ${UserSettings.ID}")
    suspend fun get(): UserSettings?

    @Upsert
    suspend fun upsert(settings: UserSettings)

    @Query("DELETE FROM user_settings")
    suspend fun deleteAll()
}
