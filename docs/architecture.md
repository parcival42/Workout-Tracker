# Architecture and data model

How the app is built and how each feature behaves. The UI calls a `Device` an *exercise* ("Übung"); code, database and backup keep the name `Device`. Only texts about the physical machine (minimum weight, increment) still say machine ("Gerät").

## Overview

- Native Android app, single Gradle module `:app`, Kotlin and Jetpack Compose (Material 3).
- Package, namespace and applicationId: `io.github.parcival42.workouttracker`. It is final: a new applicationId would be a new app on the device and lose its data. The Room schema folder in `app/schemas/` is named after the `AppDatabase` class name and must move with any package change.
- `minSdk` 30 (Android 11), `targetSdk`/`compileSdk` 37. API-gated: dynamic color from 31, in-app language picker from 33.
- Local-only persistence with Room, no network, no internet permission. The only permission is `VIBRATE` (circuit and rest timer).

## Code structure

- Single activity (`MainActivity`) hosting Compose screens.
- No DI framework: `WorkoutTrackerApplication` owns the `AppDatabase` instance; ViewModels get their DAO through a `viewModelFactory` that reads the application from `APPLICATION_KEY`.
- Packages: `data/` (entities, DAOs, `AppDatabase`, `backup/`), `ui/<feature>/` (screen plus ViewModel per feature), `ui/components/` (shared composables and formatters), `ui/theme/`.
- Navigation Compose with string routes in `ui/AppNavigation.kt`. Bottom tabs Training / History / Exercises / Settings (`TopLevelDestination`); detail screens hide the bottom bar. Each screen owns its `Scaffold`/`TopAppBar`; the outer scaffold only applies the bottom bar padding.
- `WorkoutViewModel` takes the plan id explicitly (`WorkoutViewModel.factory(planId)`, keyed per plan): the workout has no route of its own, it only lives in the Training tab.
- Drag and drop uses `sh.calvin.reorderable`: the list is reordered locally during the drag and persisted in `onDragStopped`.
- All UI text lives in `res/values/strings.xml` (English) and `res/values-de/strings.xml` (German); no hardcoded strings in Compose.
- Icons are local vector drawables in `res/drawable/`.

## Persistence

- Room with exported schemas in `app/schemas/` (committed). Every schema change has a real migration (`@AutoMigration` or `Migration`); there is no destructive fallback, because the training history is the product.
- Weights are stored as integer grams, durations as seconds, distances as meters.
- Sets logged in one tap get timestamps 1 ms apart to keep a stable order. "Last set" queries order by `performed_at DESC, id DESC`, never by id alone, because imported or restored ids are not chronological.

### Entities

- **Device** (exercise): name, `type` (`STRENGTH` / `CARDIO` / `BODYWEIGHT`, stored by name), `measure` (bodyweight: `REPS` / `TIME`), `minWeightGrams` (column `start_weight_grams`), `weightStepGrams`, `tracksDistance` (cardio), `icon` (icon key), `color` (history color, palette index or null), `archived`, `imageUri` (unused; kept to avoid a migration).
- **TrainingPlan**: name, `isDefault` (at most one, set via `PlanDao.setDefault`), `type` (`NORMAL` / `CIRCUIT`, fixed at creation), circuit settings (rounds, station/rest/countdown seconds; null for normal plans), rest timer settings (`rest_timer_enabled`, `rest_set_seconds`, `rest_exercise_enabled`, `rest_exercise_seconds`), `sessionStartedAt` (transient, see "Restart plan").
- **PlanEntry**: plan, device, sort order, and the plan's values: `weightGrams`, sets, reps, duration (columns `target_*` keep their old names), optional `stationSeconds` for circuits. A device appears in a plan at most once.
- **SetLog**: one row per performed set: device, timestamp (epoch millis), weight, reps (nullable), `durationSeconds` (nullable, timed sets), `planId` (nullable; null for quick log; `SET NULL` when the plan is deleted). A set always has reps, a duration or both.
- **CardioLog**: device, timestamp, `durationSeconds`, `distanceMeters` (nullable), `planId`. Separate table, so strength queries stay untouched.
- **UserSettings**: single row with the personal defaults (sets, reps, cardio duration).
- The per-day set number is derived at query time, not stored.

Foreign keys have no cascade: deleting a device removes its sets, sessions and plan entries in one transaction.

## Backup

`data/backup/`: JSON export and import via the Storage Access Framework, no storage permission.

- `BackupJson.encode` / `decode`, versioned (`BackupJson.VERSION`, currently 11). Older versions are read with defaults for missing fields; versions newer than the app are rejected.
- Import is fully validated before anything is deleted (sets only reference strength/bodyweight devices, sessions only cardio devices, plausible values), then replaces all data in one transaction.
- When the schema changes, `BackupJson` must be updated too, otherwise new columns are silently lost on export. A format change bumps `VERSION`.
- Not in the backup on purpose: device settings like theme and language, and the transient `sessionStartedAt`.

Version history: v6 bodyweight type and measure, v7 cardio distance, v8 plan entries store the plan's values (older files get them derived from the logs, like the schema 12 migration), v9 rest timer, v10 device icons (older files get template icons by name), v11 device colors.

## Exercises

- **Type** is chosen when creating and locked once the exercise has logged training. Cardio exercises ignore the weight fields and log `CardioLog` rows.
- **Weights**: valid machine weights form a grid `min + k * step`; +/- snap onto that grid and never go below the minimum (`ui/components/WeightSteps.kt`). The minimum is also the start weight of a new plan entry without history. Tapping a weight allows exact input (`parseKgToGrams` accepts comma and dot).
- **Bodyweight**: no weight and deliberately no added weight (with added weight, use a strength exercise). `measure` decides whether reps or time count. Logged as `SetLog` with weight 0: reps sets or timed sets.
- **Cardio distance**: `tracksDistance` can be switched any time (switching it off only stops asking; logged distances stay). Distances are shown and typed in km with comma or dot (`DistanceFormat.kt`).
- **Templates** (`ui/devices/DeviceTemplates.kt`): 20 machines in the groups Legs / Upper body / Core / Cardio, the activities Running / Cycling / Swimming (distance tracked), and 13 bodyweight exercises with a suggested measure (plank and wall sit by time, the rest by reps). Never seeded into the database: the user picks several in `TemplateDialog`, names are resolved in the current app language and then belong to the user. Templates whose name already exists (including archived exercises, case-insensitive) are disabled. Cable stations and free weights are not templates (one device = one weight history = one exercise).
- **Icons**: `ui/components/DeviceIcons.kt` is the only key -> drawable map. Keys are stored, so they are never renamed or reused; an unknown key shows the type icon. Templates carry their icon; own exercises get the type icon. There is no icon picker. Existing devices (migration 13 -> 14) and backups below v10 get the template icon when name (English or German, case-insensitive) and type match a template (frozen list in `data/TemplateIcons.kt`). Icon sources need no visible attribution (Tabler MIT, Lucide ISC, Healthicons CC0); license texts are in Settings > Open source licenses (`ui/settings/OpenSourceComponents.kt`, texts in `res/raw/`). New bundled code or icons need an entry there. Earlier type icons are kept in `docs/device-icons/previous-type-icons/`.
- **Colors**: `Device.color` is a palette index (0..7) or null for automatic, chosen in the device dialog only. See "History".
- **Archive**: exercises are archived first (archive icon next to the dialog title; undo snackbar), so their history survives. Archived exercises are reached via the archive icon in the exercise list (shown only when there are any) and can be restored (plan entries reappear) or deleted permanently; the confirmation names the number of logs that go with it.
- The exercise list is grouped by type with a type badge per row; the second line shows the last entry and how long ago, or the setup if nothing is logged yet.

## Training tab and plans

- The Training tab (`ui/training/TrainingScreen.kt`) decides by itself: no plan -> "create plan" prompt; default plan or exactly one plan -> that plan's workout starts directly; several plans without default -> plan list. A plan picked from the list becomes the tab's selected plan (saved state of the back stack entry, while the app runs); the list pops back, so the back stack never grows.
- There is no plan editor screen: a plan is built and edited in its workout. A new plan opens empty with a hint and "Add exercises". Top bar: "Switch plan" (navigation icon), "Edit plan" (`PlanSheet`: name, circuit or rest timer settings; deleting only via a red trash icon with confirmation), "Add exercises" (`AddExercisesDialog`, multi-select, exercises already in the plan disabled), overflow with "Reorder" and "Restart plan".
- Reorder mode (`ReorderList`): compact cards with drag handle and remove icon. Removing an entry shows an undo snackbar that re-inserts it with its id and order (`PlanDao.restoreEntry`); logs are never touched.
- **Plan values**: one value set per entry, no separate targets. Logging always uses these values; editing them in the card's sheet saves them in the plan; correcting a logged set via its chip never changes them. They live on the entry, not the device, because one device can be in several plans. Start values of a new entry (`newPlanEntry`): the device's last set/session anywhere, else the device minimum and the personal defaults; sets always come from the defaults. Defaults never change existing entries.
- **Restart plan** sets `sessionStartedAt` to now; the workout then counts only sets since max(midnight, `sessionStartedAt`), so a plan can be done several times a day (undo via snackbar).
- **Quick log** (`ui/quicklog/`): logs without a plan (`planId = null`). Reachable as the first row of the plan list and next to "create plan" when no plan exists. Pick any active exercise, then a sheet with weight/reps/sets (prefilled from the last set anywhere and the defaults) or duration (cardio; distance optional, never prefilled). Plans ignore these logs, but their values are start values when the exercise is added to a plan.

## Workout

- Default action is one tap "Done", which logs all still-open sets of today. The single-set button ("Set 2 of 3", then "Extra set") logs one set so the weight can change in between. Today's sets, open sets and undo are per plan; undo deletes the latest set of today on that device in this plan.
- Today's sets are chips; tapping one edits or deletes it. The next set is shown as text (`NextValueRow`); weight/reps/sets are edited in a bottom sheet (`NextSetSheet`) whose changes are saved only on "Apply" and discarded on dismiss, so stray taps on the card can't change values.
- Only the active card (first open exercise) has a filled "Done". A completed card collapses after 400 ms and ignores taps meanwhile, so a quick second tap never hits the next card. Cards show a type badge, one progress segment per target set and the undo icon next to the "x/y" counter once something was logged today.
- A bar under the top bar shows "x of n done"; a completed plan shows a summary (sets, volume = weight x reps of non-bodyweight sets, cardio time). There is no training duration: logging after the fact is intended, so timestamps say nothing about it.
- **Cardio**: "Done" logs one session with the plan duration; afterwards "Extra session". The next session's distance is prefilled from the last session with a distance (this plan, else any) and editable as a draft for the next session only. Because a prefilled result can be stale, "Done" on such an exercise shows a 10 s snackbar with "Adjust". Distance is deliberately not a plan value: it is a result, not an intention.
- Durations are shown exactly, never rounded (`formatDuration`: "20 min" or "2:30 min"); editing accepts minutes or m:ss (`parseDuration`, `DurationDialog`). +/- step one minute.

### Circuit plans

- `PlanType.CIRCUIT`, chosen at creation and fixed. Normal plans know nothing of circuits (circuit columns stay null, no circuit UI).
- Settings: rounds (default 2), station, rest and countdown times; per entry an optional own station time. A station is "do it for 2 minutes", not sets x reps.
- `CircuitPanel` above the cards: start, countdown, then every station once per round with the rest between all stations (also between rounds, not after the last); pause/resume, skip (not logged), end (logged stations stay; confirmation; the back button opens the same dialog while running).
- A finished station is logged automatically (strength: one timed set with the card's weight and the station time; cardio: one session) and stays correctable via the chips, where reps can be added. In circuit plans only the timer logs: cards have no log buttons.
- While running, the active card is highlighted and scrolled to, the screen stays on, and leaving the screen or backgrounding the app pauses the circuit (rotation does not). Starting another circuit on the same day restarts the session.
- `ui/circuit/CircuitRun.kt` is the pure, unit-tested timer (monotonic clock, no drift); `CircuitController.kt` drives it and emits sound events; `CircuitSounds` plays sine tones generated in code (`AudioTrack`, no sound files): three ticks in the last 3 s of each phase, a long tone at station start, a higher short tone at rest start, a rising chime at the end; plus vibration.

### Rest timer

- Normal plans only, switched on in the plan sheet: rest between sets (default 1:00), optionally an own rest between exercises (default 2:00).
- After a log (`chooseRest` in `ui/workout/RestChoice.kt`, pure, unit-tested): sets of the exercise still open -> set rest; exercise done (including "Done", cardio sessions and extra sets) -> exercise rest; plan done -> no rest. A new log restarts the rest; undo, "Skip" and "Restart plan" end it.
- The area under the top bar has a fixed height and crossfades between the plan progress and `RestTimerBar`, so nothing moves under the finger.
- Logs run one after another (`Mutex`), and every stop counts up a generation, so a log still being written cannot start a rest after undo or skip.
- `RestTimerController` keeps running in the background; sounds only play while the screen is started, so a rest that ran out in the background ends silently.

## History

- Custom Compose Canvas line chart (`ui/history/HistoryChart.kt`), no chart library. X axis: training days as equal slots. One point per training day and exercise: strength = top weight of the day; cardio = total minutes, total distance or km/h (total distance / total duration of sessions with a distance); bodyweight = best reps or longest hold. Single Y axis (`ValueScale`); reps only in the tap detail and the day list.
- Modes Strength / Cardio / Bodyweight as tabs (only when more than one has history); they are never mixed in one chart. Bodyweight exercises of different measures cannot be shown together.
- Filters: plan ("All plans" also shows quick logs; under a plan only exercises trained in it), range, cardio metric. The history includes archived exercises with logs.
- Below the chart: stat tiles (training days, latest value and change per exercise) and a day list with consecutive identical sets grouped ("3 x 10 · 17.5 kg").
- "All entries" (`LogEntriesScreen`) lists every set and session, newest first, grouped by day; each can be edited or deleted (undo re-inserts the row with its id).
- **Series colors**: validated categorical palette `SeriesColors` with a light and a dark set, max 8 exercises at once. Slot logic in `ui/history/SeriesColorSlots.kt` (unit-tested): an exercise with its own color always uses it, also when others share it; an automatic exercise takes the first slot no shown exercise uses and keeps it while selected; when an exercise with its own color is added, a shown automatic exercise on that color moves to a free one. An index outside the palette counts as automatic.

## Settings and appearance

- Color themes (`ui/theme/AppTheme.kt`): `DARK` (default; dynamic color from Android 12, static purple below), `LIGHT` (the same logic with light schemes), `WARM` (static warm dark scheme with an orange accent). Light or dark is decided by the theme alone, never by the system dark mode. All layout uses color roles only; colors outside the scheme (chart palette, grid) have a light and a dark set.
- The theme lives in SharedPreferences (`ThemeSettings`), read synchronously at startup. Each theme has a window style in `themes.xml`; from Android 13 it is passed to `SplashScreen.setSplashScreenTheme`, so a cold start already shows the right background (on Android 11/12 the starting window stays dark).
- Language: `res/xml/locales_config.xml` exposes English and German in the Android per-app language settings; the in-app picker uses `LocaleManager` (no AppCompat) and is hidden below Android 13. New languages go into the config and a new `values-<lang>/`.
- Settings show "Version <versionName>" and link the GitHub repository.
