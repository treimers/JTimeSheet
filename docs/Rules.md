# Funktionsweise

Die folgenden Regeln sollen im Programm (in separaten Klassen) implementiert werden. Weiterhin sollen die Testfälle entsprechend überarbeitet werden.

## Reminder

Der Reminder-Dialog erscheint nur unter bestimmten Bedingungen und es gibt Ausnahmen, die beachtet werden müssen.

### Reminder-Zeitfenster

Zur Ermittlung, ob der aktuelle Zeitpunkt im Reminder-Fenster liegt, gilt ein Regelwerk basierend auf Wochentag und Zeitraster. Nur Zeiten an den korrekten Wochentagen und innerhalb der Kernzeit können einen Reminder auslösen.

| # | Beschreibung | Expected | Test |
|---|--------------|----------|------|
| 1 | Szenario 2: Vor Fenster-Start → nicht im Fenster | `false` | `ReminderWindowRulesTest`, `isWithinWindowTestCases.json` (Fall 1) |
| 2 | Szenario 2: Nach Fenster-Ende → nicht im Fenster | `false` | `ReminderWindowRulesTest`, `isWithinWindowTestCases.json` (Fall 2) |
| 3 | Szenario 2: Kein Reminder-Wochentag (z. B. Samstag) → nicht im Fenster | `false` | `ReminderWindowRulesTest`, `isWithinWindowTestCases.json` (Fall 3) |
| 4 | Szenario 2: Innerhalb Fenster und Wochentag → im Fenster | `true` | `ReminderWindowRulesTest`, `isWithinWindowTestCases.json` (Fall 4) |

### Reminder-Intervall

Ein Reminder ist *due*, wenn alle gelten: Wochentag in `weekdays`, Uhrzeit zwischen `windowStart` und `windowEnd`, Minute auf Intervall-Grenze (z. B. :00, :15, :30, :45 bei 15 min).

| # | Beschreibung | Expected | Test |
|---|--------------|----------|------|
| 1 | Auf Intervall-Grenze und im Fenster (z. B. Mo 16:15) → due | `true` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 1) |
| 2 | Nicht auf Intervall-Grenze (z. B. 16:05 bei 15-min) → nicht due | `false` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 2) |
| 3 | Vor Fenster-Start → nicht due | `false` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 3) |
| 4 | Nach Fenster-Ende → nicht due | `false` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 4) |
| 5 | Kein Reminder-Wochentag (Samstag) → nicht due | `false` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 5) |
| 6 | Auf Grenze :00 im Fenster → due | `true` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 6) |
| 7 | Auf Grenze :15 im Fenster → due | `true` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 7) |
| 8 | Auf Grenze :30 im Fenster → due | `true` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 8) |
| 9 | Auf Grenze :45 im Fenster → due | `true` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 9) |
| 10 | Sonntag mit eigenem Fenster (settings-Override) | `true` | `ReminderIntervalRulesTest`, `isReminderDueTestCases.json` (Fall 10) |

Der Reminder-Dialog erscheint nur innerhalb des Reminder-Zeitfensters und wenn das Reminder-Intervall getroffen wurde.

## Reminder-Ausnahmen

Es gibt keinen Reminder-Dialog, wenn die folgenden Bedingungen erfüllt sind.

| # | Beschreibung | Test |
|---|--------------|------|
| 1 | Das Programm wird gestartet | `ReminderServiceTest.NoReminderOnStartup`; Implementierung: `ReminderService.start()` (kein sofortiger onTick-Aufruf) |
| 2 | Es gibt einen Eintrag, dessen Ende jetzt ist oder in der Zukunft liegt | `ReminderExceptionRulesTest.hasActivityEndingNowOrInFuture`, `shouldSuppressReminder` |
| 3 | Es gibt keine Kunde/Projekt/Task | `ReminderExceptionRulesTest.hasNoCustomerProjectTask`, `shouldSuppressReminder` |

## Add-Activity-Dialog

Der Add-Activity-Dialog soll in der Regel immer erscheinen, so dass der Benutzer jederzeit neue Zeiten erfassen kann.

### Add-Activity-Ausnahmen

Es gibt keinen Add-Activity-Dialog, wenn die folgenden Bedingungen erfüllt sind.

| # | Beschreibung | Test |
|---|--------------|------|
| 1 | Es gibt keine Kunden | `AddActivityDialogRulesTest.getBlockedReason` → `NO_CUSTOMERS` |
| 2 | Es gibt keine Kunden-Projekte | `AddActivityDialogRulesTest.getBlockedReason` → `NO_PROJECTS` |
| 3 | Es gibt keine Projekt-Tasks | `AddActivityDialogRulesTest.getBlockedReason` → `NO_TASKS` |

In diesen Fällen muss ein Hinweis-Dialog erscheinen.

### Zeitvorschläge

Im Zeiterfassungdialog (Reminder oder Add Activity) sollen Vorschläge für Kunde, Projekt, Task, Start, Ende und Länge erscheinen. **Start** ist bei den Szenarien 1 und 2 (keine vergangene Aktivität / keine Aktivität heute) der **Programmstart**, gerundet auf das konfigurierte Zeitraster; **Ende** ist jeweils **jetzt**. Diese Vorschläge sollen folgenden Regeln folgen.

| # | Szenario | Kunde | Projekt | Task | Start | Ende | Dauer | Test |
|---|----------|-------|---------|------|-------|------|-------|------|
| 1 | Keine vergangenen Aktivitäten | erster Kunde | erstes Projekt des Kunden | erste Task des Projekts | Programmstart (gerundet auf Zeitraster) | jetzt | Differenz | `reminderSuggestionLogicTestCases.json`: „Erster Kunde… Start=Programmstart (gerundet), Ende=jetzt“; „Keine Kunden → null-IDs“ |
| 2 | Keine Aktivität heute | letzter Kunde | letztes Projekt des Kunden | letzte Task des Projekts | Programmstart (gerundet auf Zeitraster) | jetzt | Differenz | `reminderSuggestionLogicTestCases.json`: „Keine Aktivität heute → letzter Kunde, Start=Programmstart…“; Szenario 7 |
| 3 | vergangene Aktivität heute | letzter Kunde | letztes Projekt des Kunden | letzte Task des Projekts | Ende der letzten Aktivität | jetzt | Differenz | `reminderSuggestionLogicTestCases.json`: „GAP von letztem Ende bis jetzt“; „Letztes Ende nicht in Zukunft…“; „Letzte Aktivität endet genau um 'jetzt'“ |
| 4 | Wechsel in Activity-Dialog | anderer Kunde | letztes Projekt des Kunden | letzte Task des Projekts | Ende der letzten Aktivität | jetzt | Differenz | `reminderSuggestionLogicTestCases.json`: „Zeitvorschlag Regel 4: Wechsel zu anderem Kunde im Dialog…“ |
| 5 | Wechsel in Activity-Dialog | Kunde unverändert | anderes Projekt des Kunden | letzte Task des Projekts | Ende der letzten Aktivität | jetzt | Differenz | `reminderSuggestionLogicTestCases.json`: „Zeitvorschlag Regel 5: Wechsel zu anderem Projekt…“ |
| 4 (Task) | Wechsel in Activity-Dialog | Kunde unverändert | Projekt unverändert | anderes Task des Projekts | Ende der letzten Aktivität | jetzt | Differenz | `reminderSuggestionLogicTestCases.json`: „Zeitvorschlag Regel 4 (Task): Kunde und Projekt unverändert…“ |

Es ist wichtig, dass bei Wechsel des Kunden oder des Projekts entweder die letzte Aktivität genommen wird oder, wenn es keine gibt, immer der erste Eintrag. (Test: Szenario 6 – Kontext-Kunde mit/ohne Aktivitäten.)

## Reminder-Dialog und Add-Activity-Dialog

Wenn der Reminder oder Add-Activity-Dialog länger als das Zeitraster geöffnet stehenbleiben, soll die Ende-Zeit bei jedem Zeitpunkt im Reminder-Intervall automatisch weitergesetzt werden, aber nie über das Kernzeitende hinaus.

Solange ein Reminder- oder Add-Activity-Dialog (oder Bearbeiten-Dialog) offen ist, darf weder ein neuer Reminder-Dialog noch ein weiterer Add-Activity-Dialog geöffnet werden; stattdessen wird nur die Endezeit im bestehenden Dialog weitergesetzt.

| Dialog offen        | Verhalten                                                                 |
|---------------------|---------------------------------------------------------------------------|
| Reminder-Dialog     | Kein neuer Reminder, kein zweiter Add-Activity; Endezeit wird im Dialog aktualisiert. |
| Add-Activity-Dialog | Kein Reminder, kein zweiter Add-Activity; Endezeit wird im Dialog aktualisiert. |
| Bearbeiten-Dialog   | Kein Reminder, kein Add-Activity (Dialog hat keinen Endezeit-Timer).      |

# Features

1. Wenn ich Kunden, Projekte oder Tasks lösche, muss ein Hinweis kommen, wenn nach Aktivitäten vorhanden sind. In diesem Falle soll es "Inkl. Aktivitäten löschen" als Option geben. Dabei muss auch der Kalender berücksichtigt werden.

1. Alle Dialoge sollen mit der Escape-Taste beendet werden können.

# Bug fixes

1. Laut docs/Rules.md soll die Ende-Zeit im Reminder-/Add-Activity-Dialog bei jedem Reminder-Intervall automatisch weitergesetzt werden, aber nie über das Kernzeitende (Reminder-Fenster-Ende) hinaus.
**Ursache**: In der Implementierung wurde die Ende-Zeit nur auf „jetzt“ (gerundet auf das Zeitraster) gesetzt, ohne Begrenzung auf das Kernzeitende. Dadurch konnte „Bis“ z.B. auf 17:15, 17:30 usw. laufen, wenn der Dialog nach 17:00 offen blieb.

1. Wenn ich die Farben der Kunden ändere, erscheinen diese erst beim Neustart im Kalender
**Ursache**: Beim Speichern im Management-Dialog wurde nur saveData() ausgeführt. Die Kalenderfarben werden aber in syncAllCalendarsFromActivities() aus den Kundendaten gelesen und per CSS gesetzt. Diese Methode lief bisher nur bei Änderungen der Activity-Liste oder beim Laden der Daten – nicht nach dem Speichern geänderter Kundeneinstellungen (z. B. Farben).
**Änderung**: Am Ende von saveData() wird jetzt syncAllCalendarsFromActivities() aufgerufen. Nach jedem Speichern (inkl. „Speichern“ im Management-Dialog) wird der Kalender damit neu aufgebaut und die aktuellen Kundenfarben (Palette/Index) werden sofort angezeigt – ohne Neustart.
