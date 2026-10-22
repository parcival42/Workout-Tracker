package io.github.parcival42.workouttracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// The device's last set (or session) in the entry's plan, else its last one anywhere. Ordered by
// time, not id: imported data does not guarantee ids in chronological order.
private const val LAST_SET_ID = """
    COALESCE(
        (SELECT s.id FROM set_log s WHERE s.device_id = plan_entry.device_id AND s.plan_id = plan_entry.plan_id
         ORDER BY s.performed_at DESC, s.id DESC LIMIT 1),
        (SELECT s.id FROM set_log s WHERE s.device_id = plan_entry.device_id
         ORDER BY s.performed_at DESC, s.id DESC LIMIT 1)
    )"""

private const val LAST_SESSION_ID = """
    COALESCE(
        (SELECT c.id FROM cardio_log c WHERE c.device_id = plan_entry.device_id AND c.plan_id = plan_entry.plan_id
         ORDER BY c.performed_at DESC, c.id DESC LIMIT 1),
        (SELECT c.id FROM cardio_log c WHERE c.device_id = plan_entry.device_id
         ORDER BY c.performed_at DESC, c.id DESC LIMIT 1)
    )"""

private const val DEVICE_TYPE = "(SELECT d.type FROM device d WHERE d.id = plan_entry.device_id)"

/**
 * Sets every plan entry to the values its workout card showed before plan entries stored them
 * (schema 11): the values of the device's last log in the plan, else anywhere, else the targets
 * (weight: the device's minimum). Only fields of that one log are used, never an older one.
 * Used by [MIGRATION_11_12] and when importing a backup from before entries had a weight.
 */
const val DERIVE_PLAN_ENTRY_VALUES_SQL = """
    UPDATE plan_entry SET
        weight_grams = CASE WHEN $DEVICE_TYPE = 'STRENGTH' THEN COALESCE(
            (SELECT l.weight_grams FROM set_log l WHERE l.id = $LAST_SET_ID),
            (SELECT d.start_weight_grams FROM device d WHERE d.id = plan_entry.device_id)
        ) ELSE 0 END,
        target_reps = CASE WHEN $DEVICE_TYPE = 'CARDIO' THEN target_reps ELSE COALESCE(
            (SELECT l.reps FROM set_log l WHERE l.id = $LAST_SET_ID), target_reps
        ) END,
        target_duration_seconds = CASE $DEVICE_TYPE
            WHEN 'CARDIO' THEN COALESCE(
                (SELECT l.duration_seconds FROM cardio_log l WHERE l.id = $LAST_SESSION_ID), target_duration_seconds
            )
            WHEN 'BODYWEIGHT' THEN COALESCE(
                (SELECT l.duration_seconds FROM set_log l WHERE l.id = $LAST_SET_ID), target_duration_seconds
            )
            ELSE target_duration_seconds
        END
    """

/** Plan entries store their weight; all values become the plan's values instead of targets. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plan_entry ADD COLUMN weight_grams INTEGER NOT NULL DEFAULT 0")
        db.execSQL(DERIVE_PLAN_ENTRY_VALUES_SQL)
    }
}
