package io.github.parcival42.workouttracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.DeviceType

@DrawableRes
fun DeviceType.iconRes(): Int = when (this) {
    DeviceType.STRENGTH -> R.drawable.ic_fitness
    DeviceType.CARDIO -> R.drawable.ic_cardio
    DeviceType.BODYWEIGHT -> R.drawable.ic_bodyweight
}

/** Each device type has its own container color, so the types are told apart at a glance. */
@Composable
private fun DeviceType.badgeColors(): Pair<Color, Color> = when (this) {
    DeviceType.STRENGTH -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    DeviceType.CARDIO -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    DeviceType.BODYWEIGHT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
}

/** Type icon in a tinted circle; decorative, the type is always named in text nearby. */
@Composable
fun DeviceTypeBadge(type: DeviceType, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val (container, content) = type.badgeColors()
    Box(
        modifier = modifier
            .size(size)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(type.iconRes()),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
