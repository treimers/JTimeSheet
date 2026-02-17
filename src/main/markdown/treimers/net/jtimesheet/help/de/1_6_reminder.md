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

## Wann greift was?

| Situation | Verhalten |
|-----------|-----------|
| **Programmstart** | Liegt die aktuelle Zeit im Reminder-Fenster (Start–Ende, Wochentag), erscheint der Reminder-Dialog **sofort** – Sie müssen nicht bis zur nächsten Intervall-Grenze warten. |
| **Während der Laufzeit** | Der nächste Reminder wird auf die **nächste Intervall-Grenze** geplant (z. B. bei 15 min: 9:00, 9:15, 9:30, …). Nur innerhalb des Fensters und an konfigurierten Wochentagen. |
| **Nach Schließen des Dialogs** | Die nächste Anzeige ist wieder zur nächsten Intervall-Grenze (z. B. Dialog um 10:15 geschlossen → nächster Reminder 10:30). |
| **Dialog bleibt offen** | Die **Endezeit** im Dialog wird automatisch mitgeführt: Bei jeder **Reminder-Intervall-Grenze** (z. B. jede Viertelstunde) wird „Bis“ auf die aktuelle Zeit (am Zeitraster ausgerichtet) gesetzt und die Dauer neu berechnet. Nur wenn das gewählte Datum **heute** ist. |
| **Außerhalb des Fensters / anderer Wochentag** | Kein Reminder; der nächste Termin wird auf den nächsten gültigen Zeitpunkt (nächster Tag bzw. Fensterbeginn) geplant. |

---

## Ablauf beim Start (Beispiel)

- **Einstellung:** Reminder 9:00–17:00, Mo–Fr, Intervall 15 min.  
- **Start um 11:04:** Sie sind im Fenster und an einem Reminder-Wochentag → der Reminder-Dialog erscheint **sofort** (nicht erst um 11:15).  
- **Start um 08:30:** Vor Fensterbeginn → erster Reminder ist um 9:00.  
- **Start am Samstag 10:00:** Kein Reminder an diesem Wochentag; erster Reminder am Montag zur nächsten Intervall-Grenze im Fenster.

---

## Dialog bleibt offen (Endezeit wird mitgeschoben)

Wenn Sie den Reminder-Dialog **nicht sofort schließen**, soll die Endezeit mit der Zeit mitgehen:

- Der Dialog öffnet z. B. um 10:15 mit Vorschlag „Bis 10:15“.  
- Sie lassen ihn offen. Um **10:30** (nächste Intervall-Grenze) wird **„Bis“ automatisch auf 10:30** gesetzt und die Dauer aktualisiert.  
- Das passiert nur, wenn das im Dialog gewählte Datum **heute** ist.  
- Der erste Update-Zeitpunkt ist die **nächste Intervall-Grenze** nach dem Öffnen (nicht einfach „Intervall Minuten nach Öffnen“). Danach alle weiteren Intervall-Grenzen, bis der Dialog geschlossen wird.

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
- **Start um 11:04** → Dialog sofort.  
- **Dialog von 10:15 bis 10:50 offen** → „Bis“ wird automatisch um 10:30 und 10:45 auf die aktuelle Zeit gesetzt (wenn Datum = heute).
