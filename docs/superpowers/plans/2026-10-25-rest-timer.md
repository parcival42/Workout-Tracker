# Rest Timer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Optional rest timer for normal plans: after a logged set or completed exercise a countdown panel in circuit style slides in, beeps like the circuit at its end and slides out.

**Architecture:** Four new columns on `TrainingPlan` (schema 13, AutoMigration, backup v9). A pure end-time class plus a small controller in `ui/resttimer/`, owned by `WorkoutViewModel`, emitting the existing `CircuitEvent`s. A pure function picks set rest vs. exercise rest. `CircuitEffects` is split so the rest timer reuses sounds and keep-screen-on without the circuit's pause-on-leave.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room 2.8.5 (KSP), JUnit 4, org.json.

**Spec:** `docs/superpowers/specs/2026-10-25-rest-timer-design.md`

## Global Constraints

- Normal plans only; circuit plans never show or use the rest timer.
- Defaults: set rest 1:00 (60 s), exercise rest 2:00 (120 s); both off by default.
- Exercise rest without its own switch = set rest.
- Sounds: `TICK` in each of the last 3 s, `STATION_START` at the end. `CircuitSounds` unchanged.
- Timer keeps running in background; sounds only while the screen is at least STARTED; no replay.
- No commits: the user commits. Leave all changes uncommitted (incl. `app/schemas/.../13.json`).
- All UI text in `res/values/strings.xml` and `res/values-de/strings.xml`; identifiers/comments English.
- Build/test with `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.

## Review Focus

- Rapid double logging (two sets within a second): the rest restarts with the newest choice, no second running timer, no stale `STATION_START` from the first.
- Switching the timer off in the plan sheet while a rest runs: the panel disappears immediately.
- Undo while a rest runs: rest ends; undo without running rest: nothing happens.
- Rest runs out while the app is in background: on return no sound, no panel.
- "Done" on the last open exercise of the plan: no rest, summary card shows as before.

---

### Task 1: Data, migration, backup

**Files:**
- Modify: `app/src/main/java/io/github/parcival42/workouttracker/data/TrainingPlan.kt`
- Modify: `app/src/main/java/io/github/parcival42/workouttracker/data/AppDatabase.kt` (version 13, `AutoMigration(from = 12, to = 13)`)
- Modify: `app/src/main/java/io/github/parcival42/workouttracker/data/backup/BackupJson.kt`
- Test: `app/src/test/java/io/github/parcival42/workouttracker/data/backup/BackupJsonTest.kt`
- Test: `app/src/androidTest/java/io/github/parcival42/workouttracker/data/MigrationTest.kt`

**Interfaces:**
- Produces on `TrainingPlan`:
  - `@ColumnInfo(name = "rest_timer_enabled", defaultValue = "0") val restTimerEnabled: Boolean = false`
  - `@ColumnInfo(name = "rest_set_seconds") val restSetSeconds: Int? = null`
  - `@ColumnInfo(name = "rest_exercise_enabled", defaultValue = "0") val restExerciseEnabled: Boolean = false`
  - `@ColumnInfo(name = "rest_exercise_seconds") val restExerciseSeconds: Int? = null`
- Produces in `TrainingPlan.kt`:
  - `data class RestTimerSettings(val enabled: Boolean = false, val setSeconds: Int = DEFAULT_SET_SECONDS, val exerciseEnabled: Boolean = false, val exerciseSeconds: Int = DEFAULT_EXERCISE_SECONDS)` with `companion object { const val DEFAULT_SET_SECONDS = 60; const val DEFAULT_EXERCISE_SECONDS = 120 }`
  - `val TrainingPlan.restTimerSettings: RestTimerSettings` (null times -> defaults)
  - `fun TrainingPlan.withRestTimerSettings(settings: RestTimerSettings): TrainingPlan` (writes all four columns)
- Backup keys: `restTimerEnabled` (bool, missing = false), `restSetSeconds`, `restExerciseEnabled`, `restExerciseSeconds` (int or null). `VERSION = 9`; add a `// v9:` line to the version comment.

- [ ] **Step 1: Write failing tests**

In `BackupJsonTest`:
- `roundTripKeepsAllData`: give one plan in the sample data `restTimerEnabled = true, restSetSeconds = 75, restExerciseEnabled = true, restExerciseSeconds = 150`; the existing equality assertion covers it.
- `readsVersion8WithRestTimerOff`: decode a v8 backup (copy the style of `readsVersion3...`) and assert `restTimerEnabled == false`, `restSetSeconds == null`, `restExerciseEnabled == false`, `restExerciseSeconds == null`.
- `rejectsNonPositiveRestSeconds`: `restSetSeconds = 0` -> `InvalidBackupException`.

In `MigrationTest`: `migrate12To13AddsRestTimerOff` — create v12 with one plan row, run `runMigrationsAndValidate(..., 13, true)`, assert `rest_timer_enabled = 0`, `rest_exercise_enabled = 0`, both seconds NULL.

- [ ] **Step 2: Run unit tests, expect failure**

Run: `./gradlew test --tests "io.github.parcival42.workouttracker.data.backup.BackupJsonTest"`
Expected: compile failure (unknown parameters) / FAIL.

- [ ] **Step 3: Implement entity fields, settings helpers, DB version, backup encode/decode/validate**

Validation message: `"Invalid rest timer settings"`; times must be `> 0` when present.

- [ ] **Step 4: Run tests**

Run: `./gradlew test --tests "io.github.parcival42.workouttracker.data.backup.BackupJsonTest"` -> PASS.
Run: `./gradlew assembleDebug` -> builds, `app/schemas/io.github.parcival42.workouttracker.data.AppDatabase/13.json` exists.
Run (emulator running): `./gradlew connectedAndroidTest --tests "*MigrationTest*"` -> PASS. If the instrumented filter is not supported, use `-Pandroid.testInstrumentationRunnerArguments.class=io.github.parcival42.workouttracker.data.MigrationTest`.

---

### Task 2: Timer, controller, rest choice

**Files:**
- Create: `app/src/main/java/io/github/parcival42/workouttracker/ui/resttimer/RestTimer.kt`
- Create: `app/src/main/java/io/github/parcival42/workouttracker/ui/resttimer/RestTimerController.kt`
- Create: `app/src/main/java/io/github/parcival42/workouttracker/ui/workout/RestChoice.kt`
- Test: `app/src/test/java/io/github/parcival42/workouttracker/ui/resttimer/RestTimerTest.kt`
- Test: `app/src/test/java/io/github/parcival42/workouttracker/ui/workout/RestChoiceTest.kt`

**Interfaces:**
- Consumes: `RestTimerSettings` (Task 1), `CircuitEvent` (existing), `StrengthItem`/`CardioItem`/`WorkoutItem` (existing).
- Produces:
  - `data class RestTimer(val endsAt: Long)` with `fun remainingMillis(now: Long): Long` (>= 0), `fun remainingSeconds(now: Long): Int` (rounded up, like the circuit), `fun isFinished(now: Long): Boolean`, `companion object { fun start(seconds: Int, now: Long): RestTimer }`.
  - `class RestTimerController(scope: CoroutineScope, clock: () -> Long)` with `val state: StateFlow<Int?>` (remaining seconds, null = no rest), `val events: SharedFlow<CircuitEvent>`, `fun start(seconds: Int)` (replaces a running rest), `fun stop()`.
  - `enum class RestKind { SET, EXERCISE }`
  - `fun chooseRest(item: WorkoutItem, loggedSets: Int, items: List<WorkoutItem>): RestKind?` — `item`/`items` are the state before logging; `loggedSets` is ignored for cardio.
  - `fun RestTimerSettings.secondsFor(kind: RestKind): Int`

- [ ] **Step 1: Write failing tests**

`RestTimerTest`:
- `remainingCountsDownFromStart`: `start(60, now = 1_000)`; `remainingSeconds(1_000) == 60`, `remainingSeconds(1_001) == 60`, `remainingSeconds(60_000) == 2`, `remainingSeconds(61_000) == 0`.
- `finishedOnlyAtEnd`: `isFinished(60_999) == false`, `isFinished(61_000) == true`, `remainingMillis(100_000) == 0`.

`RestChoiceTest` (helper builds a `StrengthItem` with `targetSets` and `n` dummy `SetLog`s, and a `CardioItem` with 0/1 sessions):
- `setRestWhileSetsOpen`: item 0/3, logged 1 -> `SET`.
- `exerciseRestWhenLastSetOfExercise`: item 2/3, logged 1, other item open -> `EXERCISE`.
- `exerciseRestOnDone`: item 0/3, logged 3, other item open -> `EXERCISE`.
- `noRestWhenPlanCompletes`: item 2/3 logged 1, all others complete -> `null`.
- `extraSetGetsSetRestEvenInCompletePlan`: item 3/3 logged 1, all others complete -> `SET`.
- `cardioGetsExerciseRestIfOtherOpen`: cardio 0/1, other open -> `EXERCISE`; all others complete -> `null`.
- `exerciseRestFallsBackToSetRest`: `RestTimerSettings(enabled = true, setSeconds = 60, exerciseEnabled = false, exerciseSeconds = 120).secondsFor(EXERCISE) == 60`; with `exerciseEnabled = true` -> `120`; `secondsFor(SET) == 60`.

- [ ] **Step 2: Run tests, expect failure**

Run: `./gradlew test --tests "*RestTimerTest" --tests "*RestChoiceTest"` -> compile failure.

- [ ] **Step 3: Implement**

`chooseRest`: strength — if `item.isComplete` before logging (extra set) -> `SET`; if `item.openSets - loggedSets > 0` -> `SET`; cardio and strength otherwise -> `EXERCISE` if any other item (`entryId != item.entryId`) is not complete, else `null`.

`RestTimerController`: same tick loop shape as `CircuitController` (`TICK_MILLIS = 100`, `BEEP_SECONDS = 3`): emit `TICK` once per second for remaining 1..3, on finish emit `STATION_START`, set state null and end the job. `start` cancels the previous job first, so a restarted rest never emits the old end tone. `stop` cancels and sets null without a sound.

- [ ] **Step 4: Run tests**

Run: `./gradlew test --tests "*RestTimerTest" --tests "*RestChoiceTest"` -> PASS.

---

### Task 3: ViewModel wiring and effects split

**Files:**
- Modify: `app/src/main/java/io/github/parcival42/workouttracker/ui/workout/WorkoutViewModel.kt`
- Modify: `app/src/main/java/io/github/parcival42/workouttracker/ui/circuit/CircuitEffects.kt`

**Interfaces:**
- Consumes: Task 1 and Task 2.
- Produces on `WorkoutUiState`: `val restTimerSettings: RestTimerSettings? = null` (null for circuit plans), `val restSeconds: Int? = null` (running rest).
- Produces on `WorkoutViewModel`: `val restTimerEvents: SharedFlow<CircuitEvent>`, `fun skipRest()`, `fun savePlan(name: String, circuitSettings: CircuitSettings?, restTimerSettings: RestTimerSettings?)` (replaces the 2-arg version).
- Produces in `CircuitEffects.kt`: `@Composable fun TimerEffects(events: Flow<CircuitEvent>, keepScreenOn: Boolean)`; `CircuitEffects` keeps its signature and calls `TimerEffects` plus its leave handling.

- [ ] **Step 1: Split effects**

`TimerEffects` owns `CircuitSounds` and keep-screen-on; it collects `events` inside `lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)` (`LocalLifecycleOwner` from `androidx.lifecycle.compose`), so nothing plays in the background. `CircuitEffects` = `TimerEffects(events, keepScreenOn)` + the existing `onLeave` part.

- [ ] **Step 2: Wire the ViewModel**

- `private val restTimer = RestTimerController(viewModelScope, clock)`; `uiState` combines `restTimer.state` into `restSeconds`.
- `workoutState` sets `restTimerSettings = plan?.takeIf { it.type == PlanType.NORMAL }?.restTimerSettings`.
- `logSets` and `logSession`: inside the launched coroutine, read `workoutState.first()` *before* inserting, insert, then if the plan's settings are enabled and `chooseRest(...)` is non-null -> `restTimer.start(settings.secondsFor(kind))`.
- `undoLastSet`, `undoLastSession`, `restartPlan`, `skipRest` -> `restTimer.stop()`.
- `savePlan`: also `withRestTimerSettings(...)` when non-null; if the saved settings are disabled -> `restTimer.stop()`.
- Circuit `logStation` never starts a rest.

- [ ] **Step 3: Build**

Update the `savePlan` call in `WorkoutScreen.kt` to pass `state.restTimerSettings` (the sheet itself changes in Task 4). Run: `./gradlew assembleDebug` -> success.

---

### Task 4: UI — plan sheet, panel, strings

**Files:**
- Create: `app/src/main/java/io/github/parcival42/workouttracker/ui/resttimer/RestTimerPanel.kt`
- Create: `app/src/main/java/io/github/parcival42/workouttracker/ui/resttimer/RestTimerSettingsFields.kt`
- Modify: `app/src/main/java/io/github/parcival42/workouttracker/ui/workout/PlanSheet.kt`
- Modify: `app/src/main/java/io/github/parcival42/workouttracker/ui/workout/WorkoutScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`

**Interfaces:**
- Consumes: Task 3 (`state.restTimerSettings`, `state.restSeconds`, `viewModel.restTimerEvents`, `viewModel.skipRest`, 3-arg `savePlan`).
- Produces:
  - `@Composable fun RestTimerPanel(remainingSeconds: Int, onSkip: () -> Unit)`
  - `@Composable fun RestTimerSettingsFields(settings: RestTimerSettings, onSettingsChange: (RestTimerSettings) -> Unit)`; `val REST_SECONDS_RANGE = 5..3_600`
  - `PlanSheet(planName, circuitSettings, restTimerSettings: RestTimerSettings?, onSave: (name, circuitSettings, restTimerSettings) -> Unit, onDelete, onDismiss)`

Strings (en / de):
- `rest_timer` "Rest timer" / "Pausentimer"
- `rest_set_time` "Rest between sets" / "Pause zwischen Sätzen"
- `rest_exercise_own` "Own rest between exercises" / "Eigene Pause zwischen Übungen"
- `rest_exercise_time` "Rest between exercises" / "Pause zwischen Übungen"
- `rest_title` "Rest" / "Pause"
- Skip button reuses `action_skip`.

- [ ] **Step 1: Settings fields and plan sheet**

`RestTimerSettingsFields`: switch row "Rest timer" (row pattern of the distance switch in `DeviceDialog.kt:120-140`, whole row toggleable); when on, `TimeField(rest_set_time, ...)` and the switch row `rest_exercise_own`; when that is on, `TimeField(rest_exercise_time, ...)`. Ranges `REST_SECONDS_RANGE`.
`PlanSheet`: saveable plain state for the four values (like the circuit ints); fields shown only when `restTimerSettings != null`, below the name; passed to `onSave`.

- [ ] **Step 2: Panel and screen**

`RestTimerPanel`: `Surface(tonalElevation = 3.dp)`, column centered, `rest_title` in `titleLarge`, `formatMinutesSeconds(remainingSeconds)` in `displayLarge`, `OutlinedButton` "Skip" full width.
`WorkoutScreen`: for normal plans `TimerEffects(viewModel.restTimerEvents, keepScreenOn = state.restSeconds != null)`; above `PlanProgress` an `AnimatedVisibility(visible = state.restSeconds != null)` with the panel (keep the last non-null value in a `remember` so the exit animation does not show 0:00 jumping); `PlanSheet` gets `state.restTimerSettings` and calls the 3-arg `savePlan`. Cards stay fully usable.

- [ ] **Step 3: Build and lint**

Run: `./gradlew assembleDebug lint` -> success, no new lint errors (missing translation would show here).

---

### Task 5: Manual check and docs

**Files:**
- Modify: `CLAUDE.md` (domain model: "Rest timer" entry; Backup line mentions v9)

- [ ] **Step 1: Emulator check** (`./gradlew installDebug` on `Medium_Phone_API_37.0`)

Plan with 2 strength exercises (3 sets) + 1 cardio, timer on with set rest 0:10, own exercise rest 0:20:
1. Single set -> 0:10 panel, ticks at 3/2/1, long tone, panel gone.
2. Last set via single set -> 0:20. "Done" on next -> 0:20. Cardio as last open -> no rest, summary card.
3. Log during a running rest -> restarts. Undo -> panel gone. Skip -> gone, no tone.
4. Start rest, home button for 30 s, return -> no panel, no late tone.
5. Plan sheet: switch timer off while running -> panel gone; switch on again -> times kept.
6. Circuit plan: sheet shows no rest fields, circuit works as before (sounds, pause on leave).

- [ ] **Step 2: Full test run**

Run: `./gradlew test` -> PASS.

- [ ] **Step 3: Update CLAUDE.md**

Add under Domain model: rest timer for normal plans (columns, defaults 1:00/2:00, choice rule, background behaviour, panel, sounds) in the existing style; Backup note v9.
