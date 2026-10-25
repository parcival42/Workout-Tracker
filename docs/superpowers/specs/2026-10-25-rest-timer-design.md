# Rest timer for normal plans

Agreed with the user on 2026-10-25.

## Goal

Normal (non-circuit) plans get an optional rest timer. After a logged set or a completed
exercise, a countdown panel in the style of the circuit panel slides in above the cards, plays
the circuit's end sounds and slides out again when the time is up. Circuit plans are untouched
and show nothing of it.

## 1. Data

`TrainingPlan` gets four columns (schema 13, `@AutoMigration(from = 12, to = 13)`):

| Column                  | Type     | Meaning                                       |
|-------------------------|----------|-----------------------------------------------|
| `rest_timer_enabled`    | Boolean, default 0 | rest timer on/off                   |
| `rest_set_seconds`      | Int?     | rest between sets (and the fallback)          |
| `rest_exercise_enabled` | Boolean, default 0 | own rest between exercises on/off   |
| `rest_exercise_seconds` | Int?     | rest between exercises                        |

- Read through `TrainingPlan.restTimerSettings` (data class `RestTimerSettings`), like
  `circuitSettings`: null times fall back to the defaults 1:00 (sets) and 2:00 (exercises).
- Switching a toggle off keeps the stored time, so switching it on again restores it.
- Circuit plans never set these columns; the rest timer is ignored for them.
- Backup: `BackupJson.VERSION` 8 -> 9, the four fields are exported and imported; older backups
  import with the timer off and null times. Validation: times, if present, are > 0.

## 2. Settings (plan sheet)

Normal plans only, below the name in `PlanSheet`:
- Switch "Rest timer". When on: a time row "Rest between sets" (m:ss, tap opens the shared
  `DurationDialog`).
- Below it, only while the rest timer is on: switch "Own rest between exercises". When on: a
  time row "Rest between exercises".
- Saved with "Save" like the name; dismissing discards changes.
- Switching the rest timer off while a rest is running ends that rest.

## 3. When a rest starts

The rest starts after the log has been written, from the item state before logging:

| Action                                         | Rest                                           |
|------------------------------------------------|------------------------------------------------|
| single set, sets of this exercise still open    | set rest                                       |
| "Extra set" (target reached, card reopened)    | exercise rest, if another exercise is open    |
| single set logs the last open set, or "Done"   | exercise rest, if another exercise is open     |
| cardio session ("Done" / "Extra session")      | exercise rest, if another exercise is open     |
| completing the last open exercise of the plan  | no rest                                        |

An "Extra set" counts as the last set of its exercise (changed with the user after testing: it is
usually the last one); in an already completed plan it gets no rest.

- "Exercise rest" uses `rest_exercise_seconds` if its switch is on, otherwise the set rest. The
  own exercise rest is only an optional deviation; by default the set rest runs everywhere.
- Bodyweight exercises behave like strength (sets).
- A new log during a running rest restarts it with the newly chosen time.
- Undo (set or session), "Skip" and "Restart plan" end the rest.
- Quick log, editing/deleting a set via its chip, and logs in circuit plans never start a rest.

## 4. Timer and sounds

- New pure class (`ui/resttimer/`) holding the end time on the monotonic clock (same clock as
  the circuit, no drift), unit-tested.
- A small controller owned by `WorkoutViewModel` ticks it, publishes the UI state (remaining
  seconds, or null when no rest runs) and emits `CircuitEvent`s through a no-replay
  `SharedFlow`: `TICK` in each of the last 3 seconds, `STATION_START` (the long tone plus
  vibration) when the rest ends. `CircuitSounds` is reused unchanged.
- The rest keeps running in the ViewModel when the tab is switched or the app goes to the
  background. Sounds are only played while the screen is at least STARTED; events emitted in the
  background are dropped (no replay). If the rest has run out on return, the panel is gone.
- The screen stays on while a rest runs.
- `CircuitEffects` is split: a reusable part (sounds, lifecycle-aware collection, keep screen
  on) used by both, and the circuit-only part that pauses the circuit when the screen is left.
  The circuit keeps its current behaviour.

## 5. Display

- With the rest timer on, the area under the top bar has a fixed height: it shows the plan
  progress and, during a rest, crossfades to `RestTimerBar` ("Rest 0:45" plus "Skip", primary
  container). Starting or ending a rest moves nothing and covers nothing. Plans without the
  rest timer keep the slim progress bar.
  (Changed with the user after testing: a circuit-style panel above the cards pushed them down,
  the time inside the progress row was too inconspicuous, and a floating bar at the bottom
  covered the lowest card.)
- No pause/resume, no abort dialog, no back handler: back behaves as without a rest.
- The cards stay fully usable during a rest (log buttons, sheets, chips, undo).
- All texts in `values/strings.xml` and `values-de/strings.xml`.

## 6. Tests

- Unit tests for the timer class (start, remaining time, restart, finish, tick seconds).
- Unit tests for the rest choice in section 3 (pure function on item state and settings).
- `MigrationTest` (androidTest) extended for 12 -> 13; `BackupJsonTest` round trip incl. a v8
  backup (timer off).
- Manual check on the emulator: both rests, restart on a new log, skip, undo, background.

## 7. Docs

CLAUDE.md gets a "Rest timer" entry in the domain model; the backup version note is updated.
