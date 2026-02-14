# Verwalten – Kunden, Projekte und Aufgaben

Unter **Verwalten** (Menü oder Symbolleiste) pflegen Sie die Hierarchie: **Kunden** → **Projekte** → **Aufgaben**. Es gibt **eine zentrale Kundenliste**; alle Änderungen wirken sofort auf dieselben Daten, die auch Filter und „Stundenzettel schreiben“ verwenden.

## Kunden

- **Hinzufügen** – Neuer Kunde. Sie können Name, Adresse und **Stundenzettel-Einstellungen** setzen: **Excel-Vorlage** (Pfad zu einer .xls- oder .xlsx-Datei), **Stundenzettel-Dateiname** (Vorschlag beim Speichern), **Tabellennummer** (Blatt im Workbook) und **Aufgaben-Trenner**. Das Layout der Vorlage (Zeilen/Spalten) wird nicht mehr pro Kunde konfiguriert, sondern aus den Platzhaltern **$month**, **$start**, **$end**, **$pause**, **$task** in der Vorlage erkannt.
- **Umbenennen** – Kundenname ändern.
- **Löschen** – Kunde entfernen (inkl. aller Projekte, Aufgaben und Aktivitäten). Sie werden über die Anzahl der Aktivitäten informiert.

Kunden auswählen und **Eigenschaften** (oder Kontextmenü) öffnen, um die Stundenzettel-Einstellungen zu bearbeiten. **Wichtig:** Der Pfad zur Excel-Vorlage muss auf eine **existierende Datei** zeigen. Beim Klick auf **Speichern** wird das geprüft; ist die Datei nicht vorhanden, erscheint eine Fehlermeldung und der Dialog bleibt geöffnet. Mit **Abbrechen** werden alle Änderungen verworfen (die Anwendung lädt den letzten Stand aus der Datei neu).

## Projekte und Aufgaben

- Unter einem Kunden können **Projekte** angelegt werden, unter einem Projekt **Aufgaben**.
- **Umbenennen** und **Löschen** funktionieren analog; beim Löschen werden alle untergeordneten Einträge und zugehörigen Aktivitäten entfernt.

Aktivitäten verweisen immer auf einen Kunden, ein Projekt und eine Aufgabe aus dieser Struktur.
