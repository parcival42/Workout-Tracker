package io.github.parcival42.workouttracker.ui.devices

import androidx.annotation.StringRes
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.defaultIcon

enum class TemplateGroup(@StringRes val labelRes: Int) {
    LEGS(R.string.template_group_legs),
    UPPER_BODY(R.string.template_group_upper_body),
    CORE(R.string.template_group_core),
    CARDIO(R.string.device_type_cardio),
    BODYWEIGHT(R.string.device_type_bodyweight),
}

/**
 * A common gym machine or bodyweight exercise offered when adding devices. The name is resolved
 * in the current app language at creation time and then belongs to the user. Weights are rough
 * defaults only: they vary by manufacturer and are meant to be adjusted. The measure of a
 * bodyweight template is a suggestion that can be changed until training is logged. The icon key
 * (see DeviceIcons) is stored with the device; templates without an own icon use the type icon.
 */
data class DeviceTemplate(
    @StringRes val nameRes: Int,
    val group: TemplateGroup,
    val type: DeviceType = DeviceType.STRENGTH,
    val minWeightGrams: Int = 5_000,
    val weightStepGrams: Int = 5_000,
    val measure: BodyweightMeasure = BodyweightMeasure.REPS,
    val tracksDistance: Boolean = false,
    val icon: String = type.defaultIcon,
)

private fun bodyweight(@StringRes nameRes: Int, measure: BodyweightMeasure = BodyweightMeasure.REPS) =
    DeviceTemplate(nameRes, TemplateGroup.BODYWEIGHT, type = DeviceType.BODYWEIGHT, minWeightGrams = 0, measure = measure)

private fun cardio(@StringRes nameRes: Int, icon: String = DeviceType.CARDIO.defaultIcon, tracksDistance: Boolean = true) =
    DeviceTemplate(nameRes, TemplateGroup.CARDIO, type = DeviceType.CARDIO, tracksDistance = tracksDistance, icon = icon)

val DeviceTemplates = listOf(
    DeviceTemplate(R.string.template_leg_press, TemplateGroup.LEGS, minWeightGrams = 10_000, icon = "leg"),
    DeviceTemplate(R.string.template_leg_extension, TemplateGroup.LEGS, icon = "leg"),
    DeviceTemplate(R.string.template_leg_curl, TemplateGroup.LEGS, icon = "leg"),
    DeviceTemplate(R.string.template_hip_abduction, TemplateGroup.LEGS, icon = "leg"),
    DeviceTemplate(R.string.template_hip_adduction, TemplateGroup.LEGS, icon = "leg"),
    DeviceTemplate(R.string.template_calf_raise, TemplateGroup.LEGS, minWeightGrams = 10_000, icon = "leg"),
    DeviceTemplate(R.string.template_chest_press, TemplateGroup.UPPER_BODY),
    DeviceTemplate(R.string.template_pec_fly, TemplateGroup.UPPER_BODY),
    DeviceTemplate(R.string.template_shoulder_press, TemplateGroup.UPPER_BODY, icon = "arm"),
    DeviceTemplate(R.string.template_lat_pulldown, TemplateGroup.UPPER_BODY, icon = "arm"),
    DeviceTemplate(R.string.template_seated_row, TemplateGroup.UPPER_BODY),
    DeviceTemplate(R.string.template_biceps_curl, TemplateGroup.UPPER_BODY, weightStepGrams = 2_500, icon = "biceps"),
    DeviceTemplate(R.string.template_triceps_extension, TemplateGroup.UPPER_BODY, weightStepGrams = 2_500, icon = "arm"),
    DeviceTemplate(R.string.template_abdominal_crunch, TemplateGroup.CORE),
    DeviceTemplate(R.string.template_back_extension, TemplateGroup.CORE),
    cardio(R.string.template_treadmill, icon = "treadmill"),
    cardio(R.string.template_elliptical),
    cardio(R.string.template_exercise_bike, icon = "bike"),
    cardio(R.string.template_rowing_machine),
    cardio(R.string.template_stair_climber, icon = "stairs", tracksDistance = false),
    cardio(R.string.template_running, icon = "run"),
    cardio(R.string.template_cycling, icon = "bike"),
    cardio(R.string.template_swimming, icon = "swimming"),
    bodyweight(R.string.template_pushups),
    bodyweight(R.string.template_situps),
    bodyweight(R.string.template_crunches),
    bodyweight(R.string.template_pullups),
    bodyweight(R.string.template_dips),
    bodyweight(R.string.template_squats),
    bodyweight(R.string.template_lunges),
    bodyweight(R.string.template_burpees),
    bodyweight(R.string.template_jumping_jacks),
    bodyweight(R.string.template_mountain_climbers),
    bodyweight(R.string.template_floor_back_extension),
    bodyweight(R.string.template_plank, BodyweightMeasure.TIME),
    bodyweight(R.string.template_wall_sit, BodyweightMeasure.TIME),
)
