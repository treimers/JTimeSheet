# Reminder-Testfälle (JSON-Spezifikation)

Die Testfälle für die Reminder-Logik liegen in JSON-Dateien. Jede Datei enthält oben `_fileDescription` und `_structure` zur Selbstdokumentation. Die Tests laden die Dateien zur Laufzeit; Keys mit Unterstrich (`_*`) werden nur für die Doku genutzt.

| Datei | Getestete Methode / Logik | Kurzbeschreibung |
|-------|---------------------------|------------------|
| **isReminderDueTestCases.json** | `ReminderService.isReminderDue(now, settings)` | Wann wird ein Reminder **ausgelöst**? (Fenster + Intervall-Grenze) |
| **isWithinWindowTestCases.json** | `ReminderSuggestionLogic.isNowWithinReminderWindow(now, settings)` | Liegt `now` im Reminder-**Fenster**? (Wochentag + Uhrzeit, ohne Intervall) |
| **reminderSuggestionLogicTestCases.json** | `ReminderSuggestionLogic.compute(…)` | **Was** wird vorgeschlagen? (Kunde, Projekt, Task, Zeitbereich, blocked, SuggestionType) |

---

## 1. isReminderDueTestCases.json

**Inhalt:** `defaultSettings` + `cases[]`

Ein Reminder ist *due*, wenn alle gelten: Wochentag in `weekdays`, Uhrzeit zwischen `windowStart` und `windowEnd`, Minute auf Intervall-Grenze (z. B. :00, :15, :30, :45 bei 15 min).

| # | description (aus JSON) | expected |
|---|------------------------|----------|
| 1 | Auf Intervall-Grenze und im Fenster (z. B. Mo 16:15) → due | true |
| 2 | Nicht auf Intervall-Grenze (z. B. 16:05 bei 15-min) → nicht due | false |
| 3 | Vor Fenster-Start → nicht due | false |
| 4 | Nach Fenster-Ende → nicht due | false |
| 5 | Kein Reminder-Wochentag (Samstag) → nicht due | false |
| 6 | Auf Grenze :00 im Fenster → due | true |
| 7 | Auf Grenze :15 im Fenster → due | true |
| 8 | Auf Grenze :30 im Fenster → due | true |
| 9 | Auf Grenze :45 im Fenster → due | true |
| 10 | Sonntag mit eigenem Fenster (settings-Override: `weekdays: ["SUNDAY"]`, eigenes Fenster) | true |

Fall 10 nutzt ein pro-Fall-`settings`-Override in der JSON; die Default-Weekdays werden durch `["SUNDAY"]` ersetzt.

---

## 2. isWithinWindowTestCases.json

**Inhalt:** `defaultSettings` + `cases[]`

Prüft nur: Wochentag in `weekdays` und Uhrzeit zwischen `windowStart` und `windowEnd`. Kein Intervall-Check (Unterschied zu isReminderDue).

| # | description (aus JSON) | expected |
|---|------------------------|----------|
| 1 | Szenario 2: Vor Fenster-Start → nicht im Fenster | false |
| 2 | Szenario 2: Nach Fenster-Ende → nicht im Fenster | false |
| 3 | Szenario 2: Kein Reminder-Wochentag (z. B. Samstag) → nicht im Fenster | false |
| 4 | Szenario 2: Innerhalb Fenster und Wochentag → im Fenster | true |

---

## 3. reminderSuggestionLogicTestCases.json (computeCases)

**Inhalt:** `defaultSettings` + `computeCases[]`

Jeder Eintrag: `setup` (customers, activities), `input` (now, lastActivityIndex, contextCustomerId, contextProjectId, fromReminder), `expected` (blockedForReminder, suggestionType, customerId, projectId, taskId, range, …).

Szenarien (nummeriert ab #1 in dieser Datei):

| Szenario | Inhalt |
|----------|--------|
| 1 | Keine Aktivität heute → letzter Kunde, jetzt–jetzt, 0:00 (DEFAULT_RANGE, Rules.md Szenario 2) |
| 2 | Keine vergangenen Aktivitäten → erster Kunde/Projekt/Task, jetzt–jetzt, 0:00 (DEFAULT_RANGE, Rules.md Szenario 1); leerer Kundenstamm → null-IDs |
| 3 | Letzte Aktivität endete in der Vergangenheit → GAP von Ende bis jetzt; Ende genau „jetzt“ → jetzt–jetzt, 0:00 (DEFAULT_RANGE) |
| 4 | Letzte Aktivität endet in der Zukunft → blockedForReminder, Range [now, now] |
| 5 | Add Activity, Ende in der Zukunft → Lücke suchen; „jetzt“ in laufender Aktivität → blocked, start=end=now |
| 6 | Kundenwechsel im Dialog → letzte Aktivität dieses Kunden bzw. erstes Projekt/Task |
| 7 | Keine Aktivität heute → jetzt–jetzt, 0:00 (DEFAULT_RANGE); Lücke → GAP (Endzeit nicht weitergesetzt) |
| (Zeitvorschlag) | **Regel 4:** Wechsel zu anderem Kunde im Dialog → letztes Projekt/Task dieses Kunden, Range Ende letzte Aktivität bis jetzt |
| (Zeitvorschlag) | **Regel 5:** Wechsel zu anderem Projekt (Kunde unverändert) → letzte Task des Projekts, Range Ende letzte Aktivität bis jetzt |
| (Zeitvorschlag) | **Regel 4 (Task):** Kunde und Projekt unverändert im Dialog → Ende letzte Aktivität bis jetzt, letzte Task (bei Wechsel Task: gleiche Logik) |

Die genaue Liste der Fälle steht in der JSON unter `computeCases[]`; das Feld `description` jedes Eintrags entspricht dem Anzeigenamen im Parameterized Test.

---

**Neue Fälle:** In der jeweiligen JSON-Datei unter `cases` bzw. `computeCases` einen neuen Eintrag mit `description`, Eingaben und `expected` anlegen – die Tests laufen automatisch darüber.

---

## Abdeckung docs/Rules.md

Jede Regel aus Rules.md ist durch mindestens einen Test abgedeckt:

| Rules.md Abschnitt | Regeln | Test-Klasse / Testressource |
|--------------------|--------|-----------------------------|
| **Reminder-Zeitfenster** | 1–4 (vor/nach Fenster, kein Wochentag, im Fenster) | `ReminderWindowRulesTest`, `isWithinWindowTestCases.json` |
| **Reminder-Intervall** | 1–10 (due, nicht due, Grenzen, Sonntag-Override) | `ReminderIntervalRulesTest`, `ReminderServiceTest`, `isReminderDueTestCases.json` |
| **Reminder-Ausnahmen** | 1: Programmstart (kein Reminder beim Start) | Implementierung in `ReminderService.start()` (kein sofortiger onTick-Aufruf); Test: `ReminderServiceTest.NoReminderOnStartup` (ohne start: tick löst nichts aus) |
| **Reminder-Ausnahmen** | 2: Eintrag Ende jetzt/Zukunft | `ReminderExceptionRulesTest.hasActivityEndingNowOrInFuture`, `shouldSuppressReminder` |
| **Reminder-Ausnahmen** | 3: Keine Kunde/Projekt/Task | `ReminderExceptionRulesTest.hasNoCustomerProjectTask`, `shouldSuppressReminder` |
| **Add-Activity-Ausnahmen** | 1–3 (keine Kunden, keine Projekte, keine Tasks) + Hinweis-Dialog (Message-Keys) | `AddActivityDialogRulesTest` |
| **Zeitvorschläge** | 1–3, 4 (anderer Kunde), 5 (anderes Projekt), 4 (anderes Task) | `ReminderSuggestionLogicTest`, `reminderSuggestionLogicTestCases.json` |

**Zuordnung Zeitvorschläge (Rules.md Tabelle) → JSON-Testfall:**

| Rules.md # | Szenario | Abgedeckt durch (description in computeCases) |
|------------|----------|-----------------------------------------------|
| 1 | Keine vergangenen Aktivitäten, erster Kunde/Projekt/Task, jetzt–jetzt, 0:00 | „Szenario 2: Erster Kunde, erstes Projekt, erste Task, jetzt–jetzt, 0:00 (Rules.md Szenario 1)“; „Szenario 2: Keine Kunden → null-IDs, jetzt–jetzt“ |
| 2 | Keine Aktivität heute, letzter Kunde/Projekt/Task, jetzt–jetzt, 0:00 | „Szenario 1: Keine Aktivität heute → letzter Kunde, jetzt–jetzt, 0:00 (Rules.md Szenario 2)“; „Szenario 7: Keine Aktivität heute → DEFAULT_RANGE“ |
| 3 | vergangene Aktivität heute, Ende letzte Aktivität bis jetzt, Differenz | „Szenario 3: Letztes Ende nicht in Zukunft…“; „Szenario 3: GAP von letztem Ende bis jetzt…“; „Szenario 3: Letzte Aktivität endet genau um 'jetzt'…“ |
| 4 | Wechsel, anderer Kunde, Ende letzte Aktivität bis jetzt | „Zeitvorschlag Regel 4: Wechsel zu anderem Kunde im Dialog…“ |
| 5 | Wechsel, anderes Projekt, Ende letzte Aktivität bis jetzt | „Zeitvorschlag Regel 5: Wechsel zu anderem Projekt (Kunde unverändert)…“ |
| 4 (Task) | Wechsel, anderes Task, Ende letzte Aktivität bis jetzt | „Zeitvorschlag Regel 4 (Task): Kunde und Projekt unverändert im Dialog…“ |

Die Regel „bei Wechsel … letzte Aktivität oder erste Eintrag“ ist in Szenario 6 (Kontext-Kunde mit/ohne Aktivitäten) abgedeckt.
