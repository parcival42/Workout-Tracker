# Play Store – Notizen

## Status

- App angelegt am 17.10.2026: Standardsprache de-DE, Übersetzung en-US, kostenlos, automatischer Schutz deaktiviert, KI-Assets gekennzeichnet.
- Geschlossener Test (Track **Alpha**) ab 17.10.2026 mit den Versionen 1.0.0 (4) bis 0.6.0 (10). Die Versionsnamen der Testphase waren nur Anzeige; `versionCode` ist durchgehend gestiegen.
- Produktionszugriff erteilt am 02.11.2026 (nach 12 Testern über 14 Tage).
- Erstes Produktions-Release: **1.0.0 (versionCode 11)**.
- 1.1.0 (versionCode 12): Übungs-Icons aus Vorlagen, feste Farben im Verlauf, Archivieren als Icon im Übungsdialog, Bearbeiten-Dialog übersteht Drehen.
- Verwaltete Veröffentlichung ist aus: Freigegebene Änderungen gehen automatisch live.

## Links

| Zweck | Link |
|---|---|
| Store-Eintrag | https://play.google.com/store/apps/details?id=io.github.parcival42.workouttracker |
| Tester werden (Opt-in, Alpha) | https://play.google.com/apps/testing/io.github.parcival42.workouttracker |
| Tester-Gruppe | https://groups.google.com/g/simple-workout-tracker-testers |
| Datenschutzerklärung | https://github.com/parcival42/Workout-Tracker/blob/main/PRIVACY.md |
| Feedback / Bugs | https://github.com/parcival42/Workout-Tracker/issues |

## Release

1. `versionCode` um 1 erhöhen (nächster: 13), `versionName` semantisch anpassen (`app/build.gradle.kts`, siehe [docs/development.md](docs/development.md)).
2. Testen: aktuelle Android-Version und Android 11 (Emulatoren `Medium_Phone_API_37.0` und `Pixel_4`), inklusive Update über die vorige Version mit Daten.
3. Release-Build auf einem Emulator ohne Debug-Build testen (R8 kann Fehler verursachen, die im Debug-Build fehlen).
4. Bundle bauen: `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew bundleRelease`
5. Datei: `app\build\outputs\bundle\release\app-release.aab`, in der Console hochladen.
6. Release-Name in der Console: `<versionCode> (<versionName>)`.
7. Versionssprung committen (`chore: bump version to X.Y.Z (versionCode N)`).

Optional: Debug-Symbole ins Bundle packen (`ndk { debugSymbolLevel }`), damit die Warnung "nativer Code ohne Symbole" verschwindet.

## Signatur

- Upload-Schlüssel: `C:\Users\parcival\.android\workouttracker-upload.jks`, Alias `upload`.
- Passwort: in KeePass (zusammen mit der `.jks` als Anhang).
- `keystore.properties` im Projekt-Wurzelverzeichnis, nicht in git.
- Play App Signing ist aktiv: Geht der Upload-Schlüssel verloren, lässt er sich über die Console ersetzen.

## Store-Material

- Texte, Icon, Feature-Grafik, Screenshots: `fastlane/metadata/android/de-DE` und `en-US`. Änderungen dort werden nicht automatisch übertragen, sondern in der Console von Hand eingetragen bzw. hochgeladen.
- Testdaten für neue Screenshots: Emulator `Release_Test`.
- Kurzbeschreibung maximal 80 Zeichen, vollständige Beschreibung maximal 4000 Zeichen.

## Antworten in der Console (App-Inhalte)

- App-Zugriff: alle Funktionen ohne besondere Zugriffsrechte
- Werbung: nein; Werbe-ID: nein
- Einstufung: "Alle anderen App-Typen", alles nein
- Zielgruppe: 18+
- Datensicherheit: keine Daten erhoben oder geteilt
- Gesundheit: Aktivitäts- und Fitness-Tracking

## Eigenes Handy (Pixel 7a)

Läuft mit der Store-Version. Wechsel von einer Debug-Version: Daten exportieren, Debug-Version deinstallieren, Store-Version installieren, Daten importieren.
