# Reminder und Zeitraster

Der **Reminder** öffnet in einstellbaren Abständen einen Dialog zum Erfassen einer Aktivität. Das **Zeitraster (Time Grid)** legt fest, auf welche Minutenwerte Zeiten in der Anwendung gerundet werden können. Beides wird unter **Einstellungen** konfiguriert.

---

## Einstellungen (Überblick)

| Einstellung | Bedeutung |
|-------------|-----------|
| **Reminder Intervall** | Abstand zwischen zwei Reminder-Terminen (z. B. 15 min = :00, :15, :30, :45). Erlaubt: 1–60 Minuten, Teiler von 60 (1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60). |
| **Reminder Start / Ende** | Tageszeitfenster, in dem der Reminder aktiv ist (z. B. 9:00–17:00). Außerhalb dieses Fensters erscheint kein Reminder. |
| **Reminder Wochentage** | An welchen Wochentagen der Reminder aktiv ist (z. B. Mo–Fr). |
| **Zeitraster (Time Grid)** | Raster für Zeiteingaben (z. B. 15 min = Zeiten nur :00, :15, :30, :45). Erlaubt: 1–60 Minuten, Teiler von 60. |

**Reminder-Intervall und Zeitraster sind unabhängig:** Das Reminder-Intervall bestimmt nur, **wann** der Dialog erscheint. Das Zeitraster bestimmt, **welche Zeiten** Sie im Dialog und in Vorschlägen wählen können (und wie die Endezeit im offenen Dialog gerundet wird). Sie können z. B. Reminder alle 15 Minuten und Zeitraster 5 Minuten verwenden.

---

## Wann erscheint der Reminder?

| Situation | Verhalten |
|-----------|-----------|
| **Programmstart** | **Kein Reminder** – beim Start erscheint kein Dialog. Der erste Reminder wird auf die **nächste Intervall-Grenze** im Fenster geplant (z. B. Start um 11:04 → erster Reminder 11:15). |
| **Während der Laufzeit** | Der nächste Reminder wird auf die **nächste Intervall-Grenze** geplant (z. B. bei 15 min: 9:00, 9:15, 9:30, …). Nur **innerhalb** des Fensters (Start–Ende) und an konfigurierten **Reminder-Wochentagen**. |
| **Außerhalb des Fensters / anderer Wochentag** | **Kein Reminder**; der nächste Termin wird auf den nächsten gültigen Zeitpunkt (nächster Tag bzw. Fensterbeginn) geplant. |
| **Letzte Aktivität endet in der Zukunft** | **Kein Reminder** – Sie befinden sich z. B. in einer laufenden Aktivität, die erst später endet. Der nächste Reminder kommt zur nächsten Intervall-Grenze. |
| **Nach Schließen des Dialogs** | Die nächste Anzeige ist wieder zur nächsten Intervall-Grenze (z. B. Dialog um 10:15 geschlossen → nächster Reminder 10:30). |

---

## Was wird im Reminder-Dialog vorgeschlagen?

Der Reminder schlägt **Kunde, Projekt, Aufgabe** und ein **Zeitintervall (Von–Bis)** vor. Die Logik:

| Situation | Vorschlag |
|-----------|-----------|
| **Keine Aktivität heute** (über alle Kunden) | **Letzter Kunde** (bzw. letzte Kundenaktivität) als Standard; falls es gar **keine vergangenen Aktivitäten** gibt: **erster Kunde**, **erstes Projekt**, **erste Aufgabe**. Zeit: **Von = jetzt − 1 Stunde**, **Bis = jetzt**. |
| **Letzte Aktivität heute endete in der Vergangenheit** | Diese Aktivität als Standard (Kunde/Projekt/Aufgabe). Zeit: **Von = Ende der letzten Aktivität**, **Bis = jetzt** (die „Lücke“ bis zum aktuellen Zeitpunkt). |
| **Kundenwechsel im Dialog** | Für den gewählten Kunden: **letzte Aktivität** dieses Kunden (auch an früheren Tagen) als Standard, sonst **erstes Projekt** und **erste Aufgabe** dieses Kunden. Zeit: gleiche Logik wie oben (Lücke oder jetzt−1h bis jetzt). |

**Hinweis:** Über **Aktivität → Aktivität hinzufügen** (ohne Reminder) gilt dieselbe Vorschlagslogik. Liegt das Ende der letzten Aktivität in der **Zukunft** (z. B. laufende Aktivität), erscheint **kein Reminder**. Beim manuellen „Aktivität hinzufügen“ wird in diesem Fall entweder die **letzte Lücke** als Vorschlag angeboten (falls es eine freie Zeit bis jetzt gibt) oder ein Dialog mit **Von = Bis = jetzt** (Dauer 0), damit Sie trotzdem eine Aktivität erfassen können.

---

## Ablauf beim Start (Beispiel)

- **Einstellung:** Reminder 9:00–17:00, Mo–Fr, Intervall 15 min.  
- **Start um 11:04:** Im Fenster und Reminder-Wochentag → **kein** Dialog sofort; **erster Reminder um 11:15**.  
- **Start um 08:30:** Vor Fensterbeginn → erster Reminder ist um 9:00.  
- **Start am Samstag 10:00:** Kein Reminder an diesem Wochentag; erster Reminder am Montag zur nächsten Intervall-Grenze im Fenster.

---

## Dialog bleibt offen (Endezeit wird weitergesetzt)

Wenn Sie den Reminder-Dialog **offen lassen** und die Endezeit auf **„Bis = jetzt“** stehen lassen:

- **Standardfall (z. B. keine Aktivität heute, Vorschlag „Von = jetzt−1h, Bis = jetzt“):**  
  Bei der **nächsten Reminder-Intervall-Grenze** wird **„Bis“ automatisch auf die neue aktuelle Zeit** gesetzt und die Dauer neu berechnet. So können Sie z. B. um 10:15 den Dialog öffnen und um 10:30 erscheint erneut mit „Bis 10:30“.
- **Wenn der Reminder eine Lücke vorschlägt** (Von = Ende der letzten Aktivität, Bis = jetzt):  
  Die **Endezeit wird nicht weitergesetzt** – beim nächsten Intervall erscheint der Dialog wieder mit derselben Lücke (gleiches Von–Bis), bis Sie eine Aktivität erfassen oder schließen.
- Das betrifft nur das **heutige** Datum.

---

## Reminder-Intervall vs. Zeitraster (Time Grid)

| Aspekt | Reminder-Intervall | Zeitraster (Time Grid) |
|--------|--------------------|-------------------------|
| **Wofür** | Zeitpunkte, zu denen der Reminder-Dialog erscheint. | Raster für Start-/Endzeiten im Dialog und für Vorschläge (z. B. „Von–Bis“). |
| **Beispiel 15 min** | Reminder um :00, :15, :30, :45. | Zeiten nur :00, :15, :30, :45 wählbar/vorgeschlagen. |
| **Unabhängig?** | Ja. Sie können z. B. Reminder alle 15 min, Zeitraster 5 min nutzen (Reminder zu Viertelstunden, Zeiteingabe in 5-Minuten-Schritten). |

---

## Kurz: Zeitachse (Beispiel)

- **Fenster:** 9:00–17:00, **Intervall:** 15 min.  
- Reminder-Termine an einem Werktag: 9:00, 9:15, 9:30, … 16:45, 17:00.  
- **Start um 11:04** → erster Reminder um 11:15 (kein Dialog sofort).  
- **Dialog mit Vorschlag „jetzt−1h bis jetzt“ von 10:15 bis 10:50 offen** → „Bis“ wird bei 10:30 und 10:45 auf die aktuelle Zeit gesetzt (wenn Datum = heute). Bei Vorschlag einer **Lücke** bleibt Von–Bis unverändert.
