# Inline plan editing (plan editor screen removed)

Agreed with the user on 2026-10-22.

## Goal

The plan editor as a full-screen page goes away. A plan is built and edited directly in the
workout. Each plan entry has exactly one stored value set (no separate "target" next to a
"prefill"), so what the card shows is what the plan says.

Problems this fixes:
- A new plan opens the editor, whose back arrow leads to the plan list instead of the plan.
- A target set in the editor (e.g. treadmill 40 min) is overridden by the last session from
  another plan or a quick log (shows 25 min), and a changed target has no effect once the device
  has history.

## 1. Creating a plan, navigation

- After `NewPlanDialog` the new plan opens directly in the (empty) workout of the Training tab.
  All three callers select the new plan: `NoPlanContent`, the embedded `PlanListScreen`, and the
  `PLANS` route (sets `SELECTED_PLAN_KEY`, pops back to Training).
- `PlanEditorScreen`, `PlanEditorViewModel` and the `plan/{planId}/edit` route are deleted.
- An empty plan shows a hint plus a button "Add exercises" (opens the add dialog, section 4).

## 2. Top bar of the workout

- Visible: "Switch plan", "Edit plan", "Add exercises".
- Overflow menu (three dots): "Reorder", "Restart plan".
- While a circuit runs, all actions stay hidden (as today).

## 3. Edit plan sheet

Bottom sheet opened by "Edit plan":
- Plan name.
- Circuit plans only: rounds, station time, rest, countdown (same fields as today's
  `CircuitSettingsDialog`).
- At the bottom: "Delete plan" with a confirmation dialog. After deleting, the Training tab falls
  back to its automatic choice.

## 4. Adding exercises

- Multi-select dialog (checkboxes, confirm "Add (n)"), styled like `TemplateDialog`.
  Exercises already in the plan are shown disabled and checked. Archived exercises are not listed.
- New entries go to the end of the plan in the order they were selected.
- Start values of a new entry:
  - weight: last set of the exercise anywhere, else its minimum weight (bodyweight: 0);
  - reps: last set's reps anywhere, else the personal default;
  - sets: personal default;
  - duration: cardio = last session anywhere, else the personal default; timed bodyweight =
    last set's duration anywhere, else `DEFAULT_HOLD_SECONDS`.

## 5. Reorder and remove

- "Reorder" (overflow) switches the list into reorder mode: all cards compact (like a collapsed
  completed card), each with a drag handle and a remove icon; all other actions on the screen are
  disabled, the plan-complete card is hidden. The top bar shows "Done" to leave the mode.
  Drag & drop uses `sh.calvin.reorderable` like the editor did; the order is saved on drag end.
- Remove also exists in the card's sheet ("Remove from plan").
- Both ways show a snackbar with "Undo". Undo re-inserts the entry with its id and values and
  restores the previous order (snapshot of the ordered entry ids, then `reorderEntries`).
- Logged sets and sessions are never touched by removing an entry.

## 6. Values

- A plan entry stores: weight, reps, sets, duration (used depending on the exercise type).
- The card's sheet edits these values and saves them to the plan entry on "Apply"; dismissing the
  sheet discards the edits (unchanged behavior, protects against stray taps).
- The in-memory drafts for weight, reps and duration are removed, including the "pin displayed
  values" logic in undo.
- "Set n of m", "Done", circuit stations: always log with the plan entry's values.
- Editing a logged set or session through its chip never changes the plan value.
- Changing the number of sets applies immediately, also to today's open sets.
- Cardio distance stays as today: prefilled from the last session with a distance (this plan,
  else anywhere), edited in the sheet as an in-memory draft only. Reason: distance is a result,
  not an intention; the "Saved ... / Adjust" snackbar stays.
- Sheet contents per type:

  | Type | Sheet |
  |---|---|
  | Strength | weight, reps, sets |
  | Bodyweight (reps) | reps, sets |
  | Bodyweight (time) | duration, sets |
  | Cardio | duration, distance (if tracked) |
  | Circuit station | weight (strength only), station time (as today) |

  Every sheet also has "Remove from plan".
- Quick log is unchanged (prefill from the last set anywhere).
- Personal defaults in Settings stay; they only feed the start values of new entries.

## 7. Data

- `PlanEntry` gets `weightGrams` (column `weight_grams`, `NOT NULL DEFAULT 0`).
- `target_sets`, `target_reps`, `target_duration_seconds` keep their names; they now are the plan
  values (no longer only targets). Kotlin property names stay as well, to keep the change small.
- Room 11 -> 12 as a manual `Migration` (an `AutoMigration` cannot compute values). It adds the
  column and sets every entry once to what its card shows today, reproducing the old ViewModel
  logic exactly:
  - pick one row: the last log of the device in this plan, else the last log anywhere
    (`ORDER BY performed_at DESC, id DESC`), from `set_log` for strength/bodyweight, from
    `cardio_log` for cardio;
  - weight = that row's weight, else the device's `start_weight_grams`; bodyweight = 0;
  - reps = `COALESCE(thatRow.reps, target_reps)` (no fallback to an older row);
  - timed bodyweight duration = `COALESCE(thatRow.duration_seconds, target_duration_seconds)`;
  - cardio duration = that row's `duration_seconds`, else `target_duration_seconds`.
  The SQL lives in one constant used by the migration and by the backup import (below).
- `BackupJson` `VERSION` 8: plan entries carry `weightGrams`. Importing a file older than v8 runs
  the same derivation as the migration after inserting, so restoring an old backup shows the same
  values as the app did when it was exported.

## 8. Tests

- Instrumented migration test (`MigrationTestHelper`, needs `androidx.room:room-testing` in the
  version matching Room 2.8.5, verified on Google Maven): last log in plan vs. anywhere, null reps
  in the last set, no history (minimum weight / target), bodyweight, cardio.
- `BackupJsonTest`: v8 round trip with `weightGrams`; decoding v7 reports the source version.
- Manual check: install over the current build in the emulator and compare cards before/after.

## 9. Cleanup

- Delete `PlanEditorScreen.kt`, `PlanEditorViewModel.kt`, and `CircuitEntryDialog` /
  `CircuitSettingsRow` if unused; delete unused strings in `values/` and `values-de/`.
- Update CLAUDE.md where it describes the editor and the "prefilled from last set" logging.
