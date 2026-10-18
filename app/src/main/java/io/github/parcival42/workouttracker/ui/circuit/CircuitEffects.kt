package io.github.parcival42.workouttracker.ui.circuit

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.Flow

/**
 * Side effects of a circuit screen: plays [events], keeps the screen on while the circuit
 * runs, and calls [onLeave] when the screen is left or the app goes to the background, so the
 * timer never logs stations unseen and without sound. A rotation does not count as leaving.
 */
@Composable
fun CircuitEffects(events: Flow<CircuitEvent>, keepScreenOn: Boolean, onLeave: () -> Unit) {
    val context = LocalContext.current
    val sounds = remember { CircuitSounds(context.applicationContext) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    LaunchedEffect(events) { events.collect(sounds::play) }

    val view = LocalView.current
    DisposableEffect(view, keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val currentOnLeave by rememberUpdatedState(onLeave)
    val activity = remember(context) { context.findActivity() }
    val leave = { if (activity?.isChangingConfigurations != true) currentOnLeave() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { leave() }
    DisposableEffect(Unit) { onDispose { leave() } }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
