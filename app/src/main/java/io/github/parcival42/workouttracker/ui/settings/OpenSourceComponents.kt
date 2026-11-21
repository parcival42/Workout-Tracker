package io.github.parcival42.workouttracker.ui.settings

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import io.github.parcival42.workouttracker.R

/** A license text bundled in res/raw; the name is the license's own name and is not translated. */
enum class OpenSourceLicense(val title: String, @RawRes val textRes: Int) {
    APACHE_2_0("Apache License 2.0", R.raw.apache_license_2_0),
    MIT_TABLER("MIT License", R.raw.license_tabler_icons),
    ISC_LUCIDE("ISC License", R.raw.license_lucide),
    CC0_HEALTHICONS("CC0 1.0 (public domain)", R.raw.license_healthicons),
}

/**
 * A bundled third-party component, what the app uses it for and its license. [id] is the route
 * argument of its license page, so it stays stable.
 */
data class OpenSourceComponent(
    val id: String,
    val name: String,
    val author: String,
    @StringRes val usageRes: Int,
    val license: OpenSourceLicense,
)

// Keep in sync with the implementation dependencies in app/build.gradle.kts and the icons in res/drawable.
val OpenSourceComponents = listOf(
    OpenSourceComponent(
        "androidx", "Android Jetpack (AndroidX): Activity, Compose, Core, Lifecycle, Navigation, Room",
        "The Android Open Source Project", R.string.licenses_use_androidx, OpenSourceLicense.APACHE_2_0,
    ),
    OpenSourceComponent(
        "kotlin", "Kotlin standard library, kotlinx.coroutines", "JetBrains s.r.o. and Kotlin contributors",
        R.string.licenses_use_kotlin, OpenSourceLicense.APACHE_2_0,
    ),
    OpenSourceComponent(
        "reorderable", "Reorderable", "Calvin Liang", R.string.licenses_use_reorderable, OpenSourceLicense.APACHE_2_0,
    ),
    OpenSourceComponent(
        "material-icons", "Material Icons", "Google", R.string.licenses_use_material_icons, OpenSourceLicense.APACHE_2_0,
    ),
    OpenSourceComponent(
        "tabler-icons", "Tabler Icons", "Paweł Kuna", R.string.licenses_use_tabler, OpenSourceLicense.MIT_TABLER,
    ),
    OpenSourceComponent(
        "lucide", "Lucide", "Lucide Icons and Contributors", R.string.licenses_use_lucide, OpenSourceLicense.ISC_LUCIDE,
    ),
    OpenSourceComponent(
        "healthicons", "Healthicons", "Resolve to Save Lives", R.string.licenses_use_healthicons,
        OpenSourceLicense.CC0_HEALTHICONS,
    ),
)
