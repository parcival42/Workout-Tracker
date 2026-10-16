# Simple Workout Tracker

A simple Android app for logging gym training on machines. Set up your devices once, put them into training plans, and log a whole exercise with a single tap. Everything is stored locally on your phone: no account, no cloud, no network access.

The app is available in English and German and always uses a dark theme.

## Features

**Devices**
- Strength and cardio devices. Strength devices have a minimum weight and a weight increment, so +/- always snaps onto the weights the machine actually offers.
- 20 templates for common machines (leg press, lat pulldown, treadmill, ...) to set up a gym in one step.
- Devices are archived instead of deleted, so their history is kept. Archived devices can be restored or deleted permanently.

**Training plans**
- Several plans, each with its own devices, target sets and reps (or target duration for cardio), ordered by drag and drop.
- A default plan starts automatically in the Training tab.
- A plan can be restarted to train it more than once a day.

**Logging**
- One tap on "Done" logs all open sets of a device. Single sets can be logged one by one if the weight changes in between.
- Weight and reps are prefilled from your last set on that device.
- "Quick log" records training outside of any plan, e.g. a spontaneous session on the treadmill.
- Every action can be undone, and logged sets can be edited or deleted.

**History**
- Line chart per device: top weight per training day for strength, total minutes per day for cardio.
- Several devices can be compared in one chart, filtered by plan and time range.
- A list of all logged entries, where each one can be edited or deleted.

**Data**
- Export and import of all data as a JSON file via the system file picker. The app needs no permissions.
- Personal defaults for new plan entries (sets, reps, cardio duration).

## Requirements

- Android 16 (API 36) or newer

## Building

The project is a single-module Gradle build with Kotlin and Jetpack Compose.

```sh
./gradlew assembleDebug     # build a debug APK
./gradlew installDebug      # install it on a connected device or emulator
./gradlew test              # run unit tests
```

A JDK 17 or newer is required. If `JAVA_HOME` is not set, the JDK bundled with Android Studio works, e.g. on Windows:

```sh
JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew assembleDebug
```

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Room for local storage, with exported schemas and real migrations
- Navigation Compose
- [Reorderable](https://github.com/Calvin-LL/Reorderable) for drag and drop
- Charts drawn with the Compose Canvas, no chart library

## Privacy

The app collects no data and has no internet access. See the [privacy policy](PRIVACY.md).

## License

[MIT](LICENSE)
