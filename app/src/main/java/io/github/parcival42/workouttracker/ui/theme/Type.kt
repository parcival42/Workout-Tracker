package io.github.parcival42.workouttracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tabular figures: weights, reps and timers keep their width while the digits change.
private fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum")

private val Base = Typography()

val Typography = Base.copy(
    displayLarge = Base.displayLarge.tabular(),
    displayMedium = Base.displayMedium.tabular(),
    displaySmall = Base.displaySmall.tabular(),
    headlineLarge = Base.headlineLarge.tabular(),
    headlineMedium = Base.headlineMedium.tabular().copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.tabular().copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.tabular(),
    titleMedium = Base.titleMedium.tabular(),
    titleSmall = Base.titleSmall.tabular(),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
)
