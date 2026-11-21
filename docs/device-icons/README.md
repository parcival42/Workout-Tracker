# Device icons (parked 2026-11-02, restored 2026-11-21)

**Status:** the patch was applied on 2026-11-21 and extended with template icons. The notes below describe the parked
state and are kept for history; `device-icons-wip.patch` is no longer needed to restore anything.

Removed from the working tree before the first production release (1.0.0), because it was
neither finished nor tested. The full state is in `device-icons-wip.patch` (24 files, incl. the
new files). Restore with:

    git apply docs/device-icons/device-icons-wip.patch

The patch is based on commit 7cdb64d (0.6.0). If it no longer applies cleanly, use
`git apply --3way`, or redo it by hand from the notes below.

## What was done

- `Device.icon` (String icon key, column `icon`, default `strength`), schema 14 via
  `AutoMigration(13 -> 14, spec = DeviceIconAutoMigration)`. The spec gives every existing
  device the key of its type (frozen SQL copy of `DeviceType.defaultIcon`).
  Exported schema `14.json`, `MigrationTest` for 13 -> 14.
- `DeviceType.defaultIcon`: `strength` / `cardio` / `bodyweight`.
- `ui/components/DeviceIcons.kt`: the only map key -> drawable. Keys are stored, so they are
  never renamed or reused; the drawable behind a key can be swapped. An unknown key shows the
  type icon (`deviceIconRes(key, type)`), with a unit test (`DeviceIconsTest`).
- `DeviceTypeBadge(type, icon)`: icon from the key, badge color still per type. The old
  `DeviceType.iconRes()` was removed. Callers: device list, workout cards (strength, cardio),
  reorder list. `PlanDao` query returns `d.icon AS deviceIcon`; `PlanEntryItem`,
  `StrengthItem` and `CardioItem` carry `deviceIcon`.
- `DeviceDialog`: changing the type takes the icon along while it is still the old type's
  own icon.
- Backup v10: devices carry `"icon"`; absent in older files -> icon of the type; unknown keys
  are kept; a blank icon fails validation. Tests in `BackupJsonTest`.
- Also in the patch (not icon-related, came along in the same work): cardio templates
  "Running" / "Cycling" / "Swimming" (de: Laufen / Fahrradfahren / Schwimmen), all with
  distance tracking, plus a `cardio()` helper in `DeviceTemplates.kt`. These hunks are in main
  since 2026-11-21 (with the type icon); exclude them when restoring:
  `git apply --exclude='*DeviceTemplates.kt' --exclude='*strings.xml' docs/device-icons/device-icons-wip.patch`

## Decisions with the user

- Icon sources only without a visible attribution: Tabler, Phosphor, Healthicons
  (game-icons is out because of CC BY).
- Tried and rejected: Material Symbols per exercise, self-drawn stick figures, and set icons
  (Tabler treadmill/bike/run/swimming were built in and removed again). For now all templates
  keep the type icon.
- No icon picker yet.

## Open

- Better icons per exercise (the reason for the per-device key).
- Picker in the device dialog.
- Test on a real device (migration from 13 with real data, backup round trip).
