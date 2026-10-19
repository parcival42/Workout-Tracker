package io.github.parcival42.workouttracker.ui.devices

import androidx.annotation.StringRes
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.DeviceType

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
 * bodyweight template is a suggestion that can be changed until training is logged.
 */
data class DeviceTemplate(
    @StringRes val nameRes: Int,
    val group: TemplateGroup,
    val type: DeviceType = DeviceType.STRENGTH,
    val minWeightGrams: Int = 5_000,
    val weightStepGrams: Int = 5_000,
    val measure: BodyweightMeasure = BodyweightMeasure.REPS,
    val tracksDistance: Boolean = false,
)

private fun bodyweight(@StringRes nameRes: Int, measure: BodyweightMeasure = BodyweightMeasure.REPS) =
    DeviceTemplate(nameRes, TemplateGroup.BODYWEIGHT, type = DeviceType.BODYWEIGHT, minWeightGrams = 0, measure = measure)

val DeviceTemplates = listOf(
    DeviceTemplate(R.string.template_leg_press, TemplateGroup.LEGS, minWeightGrams = 10_000),
    DeviceTemplate(R.string.template_leg_extension, TemplateGroup.LEGS),
    DeviceTemplate(R.string.template_leg_curl, TemplateGroup.LEGS),
    DeviceTemplate(R.string.template_hip_abduction, TemplateGroup.LEGS),
    DeviceTemplate(R.string.template_hip_adduction, TemplateGroup.LEGS),
    DeviceTemplate(R.string.template_calf_raise, TemplateGroup.LEGS, minWeightGrams = 10_000),
    DeviceTemplate(R.string.template_chest_press, TemplateGroup.UPPER_BODY),
    DeviceTemplate(R.string.template_pec_fly, TemplateGroup.UPPER_BODY),
    DeviceTemplate(R.string.template_shoulder_press, TemplateGroup.UPPER_BODY),
    DeviceTemplate(R.string.template_lat_pulldown, TemplateGroup.UPPER_BODY),
    DeviceTemplate(R.string.template_seated_row, TemplateGroup.UPPER_BODY),
    DeviceTemplate(R.string.template_biceps_curl, TemplateGroup.UPPER_BODY, weightStepGrams = 2_500),
    DeviceTemplate(R.string.template_triceps_extension, TemplateGroup.UPPER_BODY, weightStepGrams = 2_500),
    DeviceTemplate(R.string.template_abdominal_crunch, TemplateGroup.CORE),
    DeviceTemplate(R.string.template_back_extension, TemplateGroup.CORE),
    DeviceTemplate(R.string.template_treadmill, TemplateGroup.CARDIO, type = DeviceType.CARDIO, tracksDistance = true),
    DeviceTemplate(R.string.template_elliptical, TemplateGroup.CARDIO, type = DeviceType.CARDIO, tracksDistance = true),
    DeviceTemplate(R.string.template_exercise_bike, TemplateGroup.CARDIO, type = DeviceType.CARDIO, tracksDistance = true),
    DeviceTemplate(R.string.template_rowing_machine, TemplateGroup.CARDIO, type = DeviceType.CARDIO, tracksDistance = true),
    DeviceTemplate(R.string.template_stair_climber, TemplateGroup.CARDIO, type = DeviceType.CARDIO),
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
