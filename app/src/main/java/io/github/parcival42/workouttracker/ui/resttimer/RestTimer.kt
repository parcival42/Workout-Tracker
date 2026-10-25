package io.github.parcival42.workouttracker.ui.resttimer

/** A running rest that ends at [endsAt] on the monotonic clock; it never pauses. */
data class RestTimer(val endsAt: Long) {

    fun remainingMillis(now: Long): Long = (endsAt - now).coerceAtLeast(0)

    // Rounded up, so the display reaches 0 exactly when the rest ends.
    fun remainingSeconds(now: Long): Int = ((remainingMillis(now) + 999) / 1_000).toInt()

    fun isFinished(now: Long): Boolean = now >= endsAt

    companion object {
        fun start(seconds: Int, now: Long): RestTimer = RestTimer(endsAt = now + seconds * 1_000L)
    }
}
