package io.github.parcival42.workouttracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    /** Each plan entry gets the values its card showed in schema 11. */
    @Test
    fun migrate11To12DerivesPlanEntryValuesFromLogs() {
        helper.createDatabase(DB_NAME, 11).apply {
            execSQL("INSERT INTO training_plan (id, name) VALUES (1, 'A'), (2, 'B')")
            execSQL(
                """
                INSERT INTO device (id, name, archived, start_weight_grams, type, measure) VALUES
                    (1, 'Leg Press', 0, 40000, 'STRENGTH', 'REPS'),
                    (2, 'Treadmill', 0, 0, 'CARDIO', 'REPS'),
                    (3, 'Plank', 0, 0, 'BODYWEIGHT', 'TIME'),
                    (4, 'Chest Press', 0, 10000, 'STRENGTH', 'REPS'),
                    (5, 'Row', 0, 20000, 'STRENGTH', 'REPS')
                """
            )
            execSQL(
                """
                INSERT INTO plan_entry (id, plan_id, device_id, sort_order, target_sets, target_reps, target_duration_seconds) VALUES
                    (1, 1, 1, 0, 3, 10, 1200),
                    (2, 2, 1, 0, 3, 10, 1200),
                    (3, 1, 5, 1, 3, 10, 1200),
                    (4, 1, 4, 2, 3, 10, 1200),
                    (5, 1, 2, 3, 1, 10, 2400),
                    (6, 1, 3, 4, 3, 10, 30)
                """
            )
            execSQL(
                """
                INSERT INTO set_log (id, device_id, performed_at, weight_grams, reps, plan_id, duration_seconds) VALUES
                    (10, 1, 200, 50000, 8, 1, NULL),
                    (11, 1, 100, 45000, 9, 1, NULL),
                    (12, 1, 300, 60000, 6, 2, NULL),
                    (13, 5, 50, 25000, 12, NULL, NULL),
                    (14, 5, 100, 30000, NULL, NULL, 60),
                    (15, 3, 100, 0, NULL, 2, 75)
                """
            )
            execSQL("INSERT INTO cardio_log (id, device_id, performed_at, duration_seconds, plan_id) VALUES (20, 2, 100, 1500, 2)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 12, true, MIGRATION_11_12)
        val values = db.query("SELECT id, weight_grams, target_reps, target_duration_seconds FROM plan_entry ORDER BY id").use { c ->
            buildMap {
                while (c.moveToNext()) put(c.getLong(0), Triple(c.getInt(1), c.getInt(2), c.getInt(3)))
            }
        }

        // Last set in this plan by time (not id), even though plan B has a newer one.
        assertEquals(Triple(50000, 8, 1200), values[1])
        // Plan B: its own last set.
        assertEquals(Triple(60000, 6, 1200), values[2])
        // Last set anywhere has no reps (timed circuit set): reps fall back to the target, not an older set.
        assertEquals(Triple(30000, 10, 1200), values[3])
        // No history: the machine minimum and the targets.
        assertEquals(Triple(10000, 10, 1200), values[4])
        // Cardio: the last session anywhere.
        assertEquals(Triple(0, 10, 1500), values[5])
        // Timed bodyweight: no weight, the last hold.
        assertEquals(Triple(0, 10, 75), values[6])
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
