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
| 10 | Sonntag mit eigenem Fenster (settings-Override) | true |

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
| 1 | Keine Aktivität heute → letzter Kunde, Zeit now−1h bis now (DEFAULT_RANGE) |
| 2 | Keine vergangenen Aktivitäten → erster Kunde/Projekt/Task, DEFAULT_RANGE; leerer Kundenstamm → null-IDs |
| 3 | Letzte Aktivität endete in der Vergangenheit → GAP von Ende bis jetzt; Ende genau „jetzt“ → DEFAULT_RANGE |
| 4 | Letzte Aktivität endet in der Zukunft → blockedForReminder, Range [now, now] |
| 5 | Add Activity, Ende in der Zukunft → Lücke suchen; „jetzt“ in laufender Aktivität → blocked, start=end=now |
| 6 | Kundenwechsel im Dialog → letzte Aktivität dieses Kunden bzw. erstes Projekt/Task |
| 7 | Keine Aktivität heute → DEFAULT_RANGE (Endzeit weitergesetzt); Lücke → GAP (Endzeit nicht weitergesetzt) |

Die genaue Liste der Fälle steht in der JSON unter `computeCases[]`; das Feld `description` jedes Eintrags entspricht dem Anzeigenamen im Parameterized Test.

---

**Neue Fälle:** In der jeweiligen JSON-Datei unter `cases` bzw. `computeCases` einen neuen Eintrag mit `description`, Eingaben und `expected` anlegen – die Tests laufen automatisch darüber.
