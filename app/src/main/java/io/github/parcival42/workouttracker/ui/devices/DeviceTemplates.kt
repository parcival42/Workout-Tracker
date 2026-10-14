package io.github.parcival42.workouttracker.ui.devices

import androidx.annotation.StringRes
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.DeviceType

enum class TemplateGroup(@StringRes val labelRes: Int) {
    LEGS(R.string.template_group_legs),
    UPPER_BODY(R.string.template_group_upper_body),
    CORE(R.string.template_group_core),
    CARDIO(R.string.device_type_cardio),
}

/**
 * A common gym machine offered when adding devices. The name is resolved in the current app
 * language at creation time and then belongs to the user. Weights are rough defaults only:
 * they vary by manufacturer and are meant to be adjusted.
 */
data class DeviceTemplate(
    @StringRes val nameRes: Int,
    val group: TemplateGroup,
    val type: DeviceType = DeviceType.STRENGTH,
    val minWeightGrams: Int = 5_000,
    val weightStepGrams: Int = 5_000,
)

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
    DeviceTemplate(R.string.template_treadmill, TemplateGroup.CARDIO, type = DeviceType.CARDIO),
    DeviceTemplate(R.string.template_elliptical, TemplateGroup.CARDIO, type = DeviceType.CARDIO),
    DeviceTemplate(R.string.template_exercise_bike, TemplateGroup.CARDIO, type = DeviceType.CARDIO),
    DeviceTemplate(R.string.template_rowing_machine, TemplateGroup.CARDIO, type = DeviceType.CARDIO),
    DeviceTemplate(R.string.template_stair_climber, TemplateGroup.CARDIO, type = DeviceType.CARDIO),
)
