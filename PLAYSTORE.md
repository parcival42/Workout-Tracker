# Play Store – Notizen

## Status

- App angelegt am 17.10.2026: Standardsprache de-DE, Übersetzung en-US, kostenlos, automatischer Schutz deaktiviert, KI-Assets gekennzeichnet.
- Release `4 (1.0.0)` im geschlossenen Test **Alpha**, freigegeben am 17.10.2026.
- Testpflicht (privates Konto): **12 Tester gleichzeitig, 14 Tage am Stück**, danach Produktionszugriff beantragen.

## Links

| Zweck | Link |
|---|---|
| Tester-Gruppe | https://groups.google.com/g/simple-workout-tracker-testers |
| Tester werden (Opt-in) | https://play.google.com/apps/testing/io.github.parcival42.workouttracker |
| Installieren (erst nach Opt-in) | https://play.google.com/store/apps/details?id=io.github.parcival42.workouttracker |
| Datenschutzerklärung | https://github.com/parcival42/Workout-Tracker/blob/main/PRIVACY.md |
| Feedback / Bugs | https://github.com/parcival42/Workout-Tracker/issues |

## Tester: Ablauf

1. Gruppe beitreten (im Chrome/Desktop-Browser, angemeldet; nicht im In-App-Browser von Reddit).
2. Opt-in-Link öffnen, "Tester werden".
3. Nach ein paar Minuten über den Store-Link installieren.
4. Für Gruppe und Play Store **dasselbe Google-Konto** verwenden.

Probleme:
- "Content unavailable": Link wurde im In-App-Browser ohne Google-Anmeldung geöffnet.
- Keine Schaltfläche "Gruppe beitreten": nicht angemeldet oder Gruppeneinstellung nicht gespeichert.
- Notlösung: Gmail-Adresse per DM schicken lassen und in eine E-Mail-Liste im Alpha-Track eintragen.
- App im Land nicht verfügbar: Alpha-Track unter "Länder/Regionen" auf alle Länder stellen.

## Google-Gruppe: Einstellungen

- Wer kann die Gruppe sehen: Alle im Web
- Wer kann beitreten: Jeder im Web kann beitreten
- Wer kann Unterhaltungen sehen: Gruppenmitglieder (oder Alle im Web, falls der Beitritt hakt)
- Wer kann posten: Gruppenmanager
- Wer kann Mitglieder sehen: Gruppeninhaber

Beschreibung:
```
Testers for the Android app Simple Workout Tracker. After joining this group, you can install the app from the Play Store via the test link: https://play.google.com/apps/testing/io.github.parcival42.workouttracker
Please use the same Google account for this group and for the Play Store. Feedback and bug reports are welcome at https://github.com/parcival42/Workout-Tracker/issues
```

## Reddit-Beitrag (r/AndroidClosedTesting)

Titel:
```
[Test for Test] Simple Workout Tracker – free, ad-free, offline gym machine tracker
```

Text:
```
Hi everyone,

I'm a solo developer looking for testers for my first app, Simple Workout Tracker. It's a deliberately simple tracker for training on gym machines: set up your machines once, put them into plans, and log a whole exercise with one tap. History charts show your progress per machine.

- Completely free, no ads, no in-app purchases
- No account, no internet access, collects no data
- Android 11 or newer required
- English and German

How to join (please open the links in Chrome or a desktop browser, not in the Reddit app):
1. Join the tester group: https://groups.google.com/g/simple-workout-tracker-testers
2. Become a tester: https://play.google.com/apps/testing/io.github.parcival42.workouttracker
3. Install the app: https://play.google.com/store/apps/details?id=io.github.parcival42.workouttracker

Please use the same Google account for all steps and stay opted in for 14 days. Opening the app now and then helps a lot.

I'm happy to test your app in return. Just leave your links in the comments and I'll join and stay for the full 14 days.

Feedback and bug reports are very welcome, here or on GitHub: https://github.com/parcival42/Workout-Tracker/issues

Thanks!
```

## Eigenes Handy (Pixel 7a) auf Store-Version umstellen

1. In der Debug-Version: Einstellungen → Daten exportieren.
2. Gruppe beitreten, Opt-in, Debug-Version deinstallieren.
3. App aus dem Play Store installieren, Daten importieren.

## Nächster Release

1. `versionCode` erhöhen (mindestens 5), `versionName` nach Schema anpassen (`app/build.gradle.kts`).
2. Bundle bauen: `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew bundleRelease`
3. Release-APK vorher auf einem Emulator ohne Debug-Build testen (R8 kann Fehler verursachen, die im Debug-Build fehlen).
4. Datei: `app\build\outputs\bundle\release\app-release.aab`
5. Release-Name in der Console: `<versionCode> (<versionName>)`.
6. Optional: Debug-Symbole ins Bundle packen, damit die Warnung "nativer Code ohne Symbole" verschwindet.

## Signatur

- Upload-Schlüssel: `C:\Users\parcival\.android\workouttracker-upload.jks`, Alias `upload`.
- Passwort: in KeePass (zusammen mit der `.jks` als Anhang).
- `keystore.properties` im Projekt-Wurzelverzeichnis, nicht in git.
- Play App Signing ist aktiv: Geht der Upload-Schlüssel verloren, lässt er sich über die Console ersetzen.

## Store-Material

- Texte, Icon, Feature-Grafik, Screenshots: `fastlane/metadata/android/de-DE` und `en-US`.
- Testdaten für neue Screenshots: Emulator `Release_Test`.

## Antworten in der Console (App-Inhalte)

- App-Zugriff: alle Funktionen ohne besondere Zugriffsrechte
- Werbung: nein; Werbe-ID: nein
- Einstufung: "Alle anderen App-Typen", alles nein
- Zielgruppe: 18+
- Datensicherheit: keine Daten erhoben oder geteilt
- Gesundheit: Aktivitäts- und Fitness-Tracking
