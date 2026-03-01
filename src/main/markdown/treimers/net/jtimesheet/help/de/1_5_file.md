# Datei

## CSV importieren

**Datei → CSV importieren…** – Lädt Aktivitäten aus einer CSV-Datei. Die Datei sollte Spalten für Kunde, Projekt, Aufgabe, Start- und Endzeit haben. Bestehende Aktivitäten bleiben erhalten; neue werden ergänzt. Kunde, Projekt und Aufgabe müssen unter **Verwalten** bereits existieren.

## CSV exportieren

**Datei → CSV exportieren…** – Exportiert die **aktuell sichtbaren** Aktivitäten (nach Filter und aktiver Ansicht) in eine CSV-Datei. Sie wählen den Speicherort.

## Stundenzettel schreiben

**Datei → Stundenzettel schreiben…** (oder Symbolleiste) – Ist in **allen Ansichten** verfügbar. Beim Aufruf öffnet sich ein **Abfragedialog**, in dem Sie **Kunde**, **Projekt** (optional; leer = alle Projekte) und **Zeitraum** (Von/Bis) wählen. Die Anwendung filtert die Aktivitäten nach diesen Angaben und schreibt sie in einen **Excel**-Stundenzettel. Voraussetzung:

1. Im Dialog muss ein **Kunde** ausgewählt werden.
2. Der Kunde muss unter Verwalten → Eigenschaften **Stundenzettel-Einstellungen** haben: **Tabellennummer**, **Excel-Vorlage** (Pfad zu einer vorhandenen .xls- oder .xlsx-Datei) und optional **Stundenzettel-Dateiname** und **Aufgaben-Trenner**.

Die Anwendung füllt die Vorlage mit den Aktivitätsdaten (Datum, Zeiten, Pausen, Aufgaben) und Sie speichern die resultierende Datei. **Format:** Die Erweiterung der Zieldatei (.xls oder .xlsx) richtet sich immer nach der Vorlage; der vorgeschlagene Dateiname verwendet die gleiche Erweiterung.

**Layout der Vorlage:** Das Layout (wo Monat, Start, Ende, Pause, Aufgaben stehen) wird aus Platzhaltern im Template erkannt: **$month**, **$start**, **$end**, **$pause**, **$task**. Diese Marken müssen in der Excel-Vorlage in der ersten Datenzeile stehen. Fehlen sie, erscheint eine Fehlermeldung – es gibt keine manuelle Zeilen-/Spaltenkonfiguration mehr pro Kunde. Leere Pausen werden als **0:00** geschrieben. Formeln in der Vorlage werden vor dem Speichern ausgewertet.

<img src="../timesheet-template-mockup.png" alt="Excel-Vorlage mit Platzhaltern" width="421" height="439">

Das ergibt beim Schreiben folgenden Stundenzettel:

<img src="../timesheet.png" alt="Stundenzettel" width="421" height="439">

**Fehlerfälle:** Ist keine Vorlage hinterlegt oder die Vorlagendatei existiert nicht, erscheint eine Fehlermeldung (kein Dialog zum Nachwählen der Datei). Die Vorlage muss unter Verwalten korrekt und auf eine existierende Datei zeigend eingetragen sein.
