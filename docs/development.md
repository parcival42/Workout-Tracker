# Development

## Setup

- Android Studio with its bundled JDK (`C:\Program Files\Android\Android Studio\jbr` on Windows) or any JDK 17+.
- Commands are listed in the [README](../README.md#building).
- Library versions live in `gradle/libs.versions.toml`. Check new versions against Google Maven / Maven Central before adding or bumping a dependency. AGP 9 with built-in Kotlin (no `kotlin-android` plugin), KSP for Room.

## Versioning

- `versionName` and `versionCode` are set by hand in `app/build.gradle.kts` (`defaultConfig`), only for a release.
- `versionName` is semantic (MAJOR.MINOR.PATCH): PATCH for bug fixes, MINOR for new features, MAJOR for big breaks. 1.0.0 is the first production release.
- `versionCode` increases by 1 with every release; Android rejects updates with a lower or equal code.
- Debug builds get the suffix `-debug`.

## Database changes

1. Change the entity and bump `version` in `AppDatabase`.
2. Add an `AutoMigration` (with a spec if data has to be filled) or a manual `Migration`. Never a destructive fallback.
3. Build once: Room exports the new schema JSON to `app/schemas/`. Commit it. Bump the version *before* the first build, otherwise the export overwrites the previous schema file.
4. Add a test to `app/src/androidTest/.../MigrationTest.kt` and run `./gradlew connectedAndroidTest` (this uninstalls the app from the test device afterwards).
5. Update `BackupJson` (encode, decode, validation) and bump its `VERSION` if the format changes, with a test in `BackupJsonTest` for reading the previous version.

## Conventions

- Code, identifiers and comments in English. UI texts in English and German, always both (`values/` and `values-de/`).
- Logic that can be pure (timers, slot assignment, rest choice, formatting) lives in plain Kotlin files with unit tests.
- Commit messages start with a conventional-commit type (`feat:`, `fix:`, `docs:`, `chore:`, ...).
- New bundled code or icons need an entry in Settings > Open source licenses (`ui/settings/OpenSourceComponents.kt`, license text in `res/raw/`).

## Testing

- Unit tests: `./gradlew test`.
- Migrations: `./gradlew connectedAndroidTest` on an emulator.
- Before a release, test on a current Android version and on Android 11 (minSdk), including an update over the previous version with real data, and test the release build (R8) on an emulator; see [PLAYSTORE.md](../PLAYSTORE.md).
