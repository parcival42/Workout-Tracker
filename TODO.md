# Open points and ideas

## Ideas

- Cardio distance, step 2: distance as plan target ("run 3 km today"), chosen per plan entry
  (duration or distance) for exercises that track distance. Open: the duration is then the
  result and has the same stale-prefill problem as the distance today; km/h needs both.
  Related: should the duration stay required (quick log), or can a session be logged with only
  a distance when the time does not matter?
- Better icons per exercise for own exercises, possibly an icon picker in the exercise dialog
  (today icons only come from templates or the type).

## Known issues

Found in emulator tests (Medium Phone API 37 / Pixel 4 API 30). Each needs a decision first.

### Bugs / UX problems

1. German template names are easy to mix up: "Rudermaschine" (seated row, strength) vs.
   "Rudergerät" (cardio), "Rückenstrecker" (machine) vs. "Rückenstrecken" (bodyweight).
   Idea: "Rudern sitzend" / "Ruderzug" and "Rückenstrecken am Boden".
2. The "Own exercise" dialog has no duplicate name check: "leg press" next to "Leg press" keeps
   Save enabled, while templates check case-insensitively. Decide whether this is intended.
3. Import asks "Replace all data?" before the file is validated and without saying what it
   contains. Idea: validate first, then confirm with counts (exercises, plans, logs).
4. Circuit in landscape: the circuit panel plus the bottom bar fill the whole screen, the
   cards are not reachable.
5. Template dialog rows are only about 28 dp high (Material recommends 48 dp touch targets).
6. The set edit dialog only disables Save for invalid values, without an error text (the
   exercise dialog shows one).
7. Day rollover while the app keeps running: 1.0.0 once still showed the sets of Oct 16 as
   "today" on Oct 18 (process kept alive by an emulator snapshot); after an app restart it was
   correct. If the app stays open over midnight, "today" (open sets, counters) probably does
   not switch. Not verified since. Idea: derive the day start from a clock flow that ticks at
   midnight / on resume instead of computing it once.
8. The history selection is keyed by exercise id and survives a backup import while the app
   runs, so an imported exercise can inherit the slot (color) of the previous one with the
   same id. Gone after an app restart. Idea: reset the selection after an import.

### Design questions

9. Duration steppers step by whole minutes, also for bodyweight holds: 0:30 -> + -> 1:30 ->
   - -> 1 min; 0:30 can only be restored by typing. 15 s steps may fit holds better.
10. The circuit timer keeps running while the "End circuit?" dialog is open.

### Not covered on Android 11 yet

Quick log, all entries, chip editing, deleting exercises, drag and drop, multi-day chart.
Import/export work on API 30 (file taps in the emulator's picker sometimes do not register,
keyboard selection does); worth one manual import on a real Android 11 device.
