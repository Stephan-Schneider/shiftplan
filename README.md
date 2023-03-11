# shiftplan

**shiftplan** ist eine Anwendung zum Erstellen von Spätschicht- und Homeofficeplänen für eine kleine Abteilung mit 
folgenden Anforderungen:

* Jeder Mitarbeiter hat Anspruch auf eine fest definierte Anzahl von Homeoffice-Tagen.
* Alle oder eine Teilmenge der Mitarbeiter stehen in abwechselnder Reihenfolge für eine definierte Anzahl von 
Spätschichttagen zur Verfügung. Die Anzahl der Spätschichttage muss innerhalb eines gegebenen Zeitraums gleich auf 
die MA's verteilt werden.
* Eine Spätschicht muss in Präsenz im Büro durchgeführt werden.
* Der Schichtplan kann für eine beliebige Zeitspanne innerhalb eines Kalenderjahres erstellt werden.

---
## Technische Anforderungen
Die Ausführung der Anwendung erfordert **Java SE 17** 

---
## Installation und Verwendung

* Entpacken des shiftplan.zip - Archivs in ein beliebiges Verzeichnis
* Nach dem Entpacken enthält das Installationsverzeichnis die Unterordner _'lib'_, _'XML'_ und _'Template'_
* Aufruf der Anwendung durch das Shellskript shiftplan.sh, das sich direkt im Installationsverzeichnis befindet.

Die Datei shiftplan.sh kann editiert werden, um folgende Parameter als Vorgabe-Einstellungen zu setzen:
1. **xmlPath:** Pfad zum Ordner, der die Konfigurationsdatei shiftplan.xml und die Schema-Datei shiftplan.xsd enthält.
In der Voreinstellung ist dies der Ordner \<Installationsverzeichis\>/XML. Die XML- und Schema-Datei können in 
ein beliebiges, anderes Verzeichnis verschoben werden, der Pfad ist in diesem Fall entsprechend anzupassen. Beide
Dateien müssen sich allerdings immer im gleichen Verzeichnis befinden!
2. **templatePath:** Pfad zum Ordner, der das Template shiftplan.ftl enthält. In der Voreinstellung ist das der Ordner 
\<Installationsverzeichnis\>/Template. Das CSS- und HTML-Markup des Templates können editiert werden.
3. **configPath:** Der Pfad zur SMTP-Konfigurationsdatei. Die Datei muss folgendes Format haben:
```
from=name.lastname@example.net # Email-ID
host=mail.server.com # SMTP-Server des Email-Providers
```

Alle Parameter können auch auf der Kommandozeile übergeben werden und überschreiben dann die Vorgaben in shiftplan.sh.
Die Parameter `smtpPassword` und `sendMail` müssen immer bei Aufruf angegeben werden, falls ein Emailversand des 
Schichtplans durch die Anwendung durchgeführt werden soll.

---
## shiftplan.xml
Die Parameter für die Erstellung des Schichtplans werden in **shiftplan.xml** festgelegt.\
Das root-Element `shiftplan` hat ein Pflichtattribut `for`, das Jahr, in welchem der Schichtplan gelten soll enthaltend.
Die optionalen Attribute `start-date` und `end-date` grenzen den Zeitraum innerhalb dieses Jahres weiter ein; das
Datum sollte immer auf den Anfang oder Ende eines Monats fallen.\
Das Element `public-holidays` schließt eine Sequenz von `holiday`-Elementen ein, mit denen gesetzliche Feiertage, die
auf einen Werktag fallen, registriert werden.\
Im Abschnitt `shift-duration` werden folgende Parameter festgelegt:
- `max-home-per-week`: die maximal erlaubte Anzahl von Homeoffice-Tagen pro Woche.
- `max-home-per-month`: die maximal erlaubte Anzahl von Homeoffice-Tagen pro Monat.
- `home-office`: Homeoffice-Zyklus (Anzahl aufeinanderfolgender Tage, an welchen der MA optional von zu Hause arbeiten kann).
- `max-late-shift`: Anzahl der aufeinanderfolgenden Spätschichttage eines MA's. Je nach Aufteilung der Schichten kann
die tatsächliche Anzahl der Tage geringer sein.
- `employee-groups`: Für die Verteilung der Homeoffice-Tage werden die MA's in Gruppen eingeteilt. Jede Homeoffice-Gruppe
hat einen eindeutigen Namen und eine Sequenz von einem oder mehreren Mitarbeitern
- `employee`: Jeder MA wird mit Namen, Nachnamen, E-Mail, einer `boolean`-Flagge für nur Spätschichtteilnahme, der 
Position des MA's bei der Verteilung der Spätschichten (die Reihenfolge hängt von der jeweiligen Homeoffice-Gruppe ab)
und einer Farbe zur Hervorhebung des MA's im Schichtplan konfiguriert.

Weitere Details über die erlaubten XML-Elemente und Attribute, sowie deren Anordnung und die Daten, die sie enthalten 
dürfen, können der Schema-Datei **shiftplan.xsd** entnommen werden.

---

## Lizenz
MIT-License (siehe LICENSE.txt im Installationsverzeichnis)

