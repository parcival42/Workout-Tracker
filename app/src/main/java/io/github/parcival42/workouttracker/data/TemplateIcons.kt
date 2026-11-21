package io.github.parcival42.workouttracker.data

/**
 * Icon key for devices that existed before devices had icons (database migration 13 -> 14 and
 * backups below v10): a device whose name is a template name (English or German, as of schema 14)
 * and whose type matches the template gets that template's icon.
 *
 * Frozen copy, deliberately not read from the templates or string resources: renaming a template
 * later must not change what the migration does.
 */
private val LEGACY_TEMPLATE_ICONS: List<Triple<DeviceType, String, List<String>>> = listOf(
    Triple(DeviceType.STRENGTH, "leg", listOf(
        "Leg press", "Beinpresse", "Leg extension", "Beinstrecker", "Leg curl", "Beinbeuger",
        "Hip abduction (outer)", "Abduktor (außen)", "Hip adduction (inner)", "Adduktor (innen)",
        "Calf raise", "Wadenmaschine",
    )),
    Triple(DeviceType.STRENGTH, "arm", listOf(
        "Shoulder press", "Schulterpresse", "Lat pulldown", "Latzug", "Triceps extension", "Trizepsmaschine",
    )),
    Triple(DeviceType.STRENGTH, "biceps", listOf("Biceps curl", "Bizepsmaschine")),
    Triple(DeviceType.CARDIO, "treadmill", listOf("Treadmill", "Laufband")),
    Triple(DeviceType.CARDIO, "bike", listOf("Exercise bike", "Ergometer", "Cycling", "Fahrradfahren")),
    Triple(DeviceType.CARDIO, "stairs", listOf("Stair climber", "Stepper")),
    Triple(DeviceType.CARDIO, "run", listOf("Running", "Laufen")),
    Triple(DeviceType.CARDIO, "swimming", listOf("Swimming", "Schwimmen")),
)

private val legacyIconByTypeAndName: Map<Pair<DeviceType, String>, String> =
    LEGACY_TEMPLATE_ICONS.flatMap { (type, icon, names) -> names.map { (type to it.lowercase()) to icon } }.toMap()

/** The icon a device without a stored icon gets: its template's icon if the name matches, else its type's. */
fun legacyDeviceIcon(name: String, type: DeviceType): String =
    legacyIconByTypeAndName[type to name.trim().lowercase()] ?: type.defaultIcon
