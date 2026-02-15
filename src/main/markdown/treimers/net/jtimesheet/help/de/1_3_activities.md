# Aktivitäten

Aktivitäten sind Zeiteinträge mit Kunde, Projekt, Aufgabe sowie Start- und Endzeit.

## Aktivität hinzufügen

- **Menü:** Aktivität → Aktivität hinzufügen  
- **Symbolleiste:** Auf „Hinzufügen“ klicken  
- **Kontextmenü:** Rechtsklick in der Tabelle → Aktivität hinzufügen  

Im Dialog Kunde, Projekt und Aufgabe wählen, dann **Von** und **Bis** setzen. Die Dauer wird automatisch berechnet. Mit **Speichern** bestätigen.

## Bearbeiten und Löschen

- **Bearbeiten:** Zeile auswählen und Aktivität → Aktivität bearbeiten (oder Doppelklick / Kontextmenü).
- **Löschen:** Zeile auswählen und Aktivität → Aktivität löschen (oder Kontextmenü). Löschung bestätigen.

## Pause einfügen

Soll eine Aktivität geteilt werden (z. B. wegen einer Pause), die Aktivität auswählen und **Aktivität → Pause einfügen** wählen. Im Dialog **Pause von** und **Pause bis** innerhalb der Aktivitätszeit setzen. Die Aktivität wird in zwei Einträge geteilt: einer endet zum Pausenbeginn, der andere beginnt am Pausenende. Die Pause selbst wird nicht als eigener Eintrag gespeichert, sondern ist nur die Lücke zwischen den beiden entstandenen Aktivitäten.

## Zeitüberschneidung (Konflikt)

Beim **Hinzufügen** oder **Bearbeiten** einer Aktivität prüft die Anwendung, ob die gewählte Zeit mit bestehenden Aktivitäten (über alle Kunden hinweg) kollidiert. Bei Überschneidung erscheint der Dialog **Zeitüberschneidung**:

- Er listet alle Aktivitäten, die mit Ihrer gewählten Zeit kollidieren.
- **Trotzdem übernehmen** übernimmt die eingegebene Zeit (Überschneidungen bleiben bestehen).
- **Dahinter verschieben** bzw. **Davor verschieben** schlagen einen freien Slot (gleiche Dauer) vor und zeigen den genauen Zeitraum. Mit der Auswahl wird dieser Slot übernommen; der Dialog erscheint nicht erneut.
- **Abbrechen** schließt den Overlap-Dialog und bringt Sie zurück in den Dialog „Aktivität hinzufügen“ bzw. „Aktivität bearbeiten“ mit Ihren zuletzt eingegebenen Werten, damit Sie die Zeit anpassen oder dort abbrechen können.

Die Overlap-Prüfung läuft nur beim Speichern aus dem Hinzufügen- oder Bearbeiten-Dialog. Die beim Öffnen von „Aktivität hinzufügen“ (oder beim Wechsel von Kunde/Projekt) vorgeschlagenen Zeiten werden nicht im Voraus geprüft.

## Konsolidieren

Wenn mehrere kurze Aktivitäten zusammengehören (z. B. gleiche Aufgabe, hintereinander), Bereich auswählen und **Konsolidieren** nutzen. Die Aktivitäten werden zu einer zusammengefasst (erster Start, letztes Ende). Sinnvoll vor dem Schreiben des Stundenzettels.
