# Open points and ideas

## Ideas

- Rename the "Devices" tab ("Geräte") to a term that covers machines as well as bodyweight
  exercises. Rejected so far: "Übungen" (sounds like practising, not training hard),
  "Stationen" (does not fit bodyweight exercises at home). Code keeps the name `Device`;
  a rename also touches the store texts in `fastlane/metadata/` and the screenshots.
- Cardio distance, step 2: distance as plan target ("run 3 km today"), chosen per plan entry
  (duration or distance) for devices that track distance. Open: the duration is then the
  result and has the same stale-prefill problem as the distance today; km/h needs both.
  Related: should the duration stay required (quick log), or can a session be logged with only
  a distance when the time does not matter?

## Findings from the emulator test (2026-10-18, Medium Phone API 37 / Pixel 4 API 30)

Not fixed yet; each needs a decision first.

### Bugs / UX problems

1. German template names are easy to mix up: "Rudermaschine" (seated row, strength) vs.
   "Rudergerät" (cardio), "Rückenstrecker" (machine) vs. "Rückenstrecken" (bodyweight).
   Idea: "Rudern sitzend" / "Ruderzug" and "Rückenstrecken am Boden".
2. The next-set sheet subtitle says "Weight and reps for the next set"
   (`next_set_sheet_subtitle`) for bodyweight exercises too, where there is no weight
   (push-ups) or neither weight nor reps (plank). Idea: one subtitle per measure.
3. The "Own device" dialog has no duplicate name check: "leg press" next to "Leg press" keeps
   Save enabled, while templates check case-insensitively. Decide whether this is intended.
4. Import asks "Replace all data?" before the file is validated and without saying what it
   contains. Idea: validate first, then confirm with counts (devices, plans, logs).
5. Circuit in landscape: the circuit panel plus the bottom bar fill the whole screen, the
   cards are not reachable.
6. Template dialog rows are only about 28 dp high (Material recommends 48 dp touch targets).
7. The set edit dialog only disables Save for invalid values, without an error text (the
   device dialog shows one).
8. Day rollover while the app keeps running: in the release test, 1.0.0 still showed the sets
   of Oct 16 as "today" on Oct 18 (process kept alive by an emulator snapshot); after an app
   restart it was correct. If the app stays open over midnight, "today" (open sets, counters)
   probably does not switch. Not verified in 1.1.0. Idea: derive the day start from a clock
   flow that ticks at midnight / on resume instead of computing it once.

### Design questions

9. Duration steppers step by whole minutes, also for bodyweight holds: 0:30 -> + -> 1:30 ->
   - -> 1 min; 0:30 can only be restored by typing. 15 s steps may fit holds better.
10. The circuit timer keeps running while the "End circuit?" dialog is open.

### Docs

11. CLAUDE.md says templates are a top bar action in the device list; they are actually
    reached via the FAB (choice sheet) and the empty state button.

### Not covered on the Pixel 4

Quick log, all entries, chip editing, archive/delete, drag & drop, rotation, multi-day chart.
Import/export work on API 30 (file taps in the emulator's picker did not register, keyboard
selection did); worth one manual import on a real Android 11 device before the release.

