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

### shiftplan.sh
Aufruf der Hilfefunktion mit dem Parameter **-h**
Das Skript unterstützt folgende Parameter:
* **-x:** Pfad zum Ordner, der die Konfigurationsdatei shiftplan.xml und die Schema-Datei shiftplan.xsd enthält. In der
Voreinstellung ist dies der Ordner \<Installationsverzeichis\>/XML. Die XML- und Schema-Datei können jedoch in 
ein beliebiges, anderes Verzeichnis verschoben werden, der Pfad ist in diesem Fall mit der Option **-x** entsprechend 
anzugeben. Beide Dateien müssen sich allerdings immer im gleichen Verzeichnis befinden.
* **-t:** Pfad zum Ordner, der das Template shiftplan.ftl enthält. In der Voreinstellung ist das der Ordner
\<Installationsverzeichnis\>/Template. Falls das Template in ein anderes Verzeichnis verschoben wird, ist das neue
Verzeichnis mit der Option **-t** anzugeben.
* **-c:** Der Pfad zur SMTP-Konfigurationsdatei - befindet sich im Installationsverzeichnis und kann ebenfalls verschoben
werden, wobei der neue Pfad zur Konfigurationsdatei mit der Option **-c** anzugeben ist.
* **-s** Diese Option ist anzugeben, wenn ein automatisierter Emailversand des erstellten Schichtplans durch die 
Anwendung durchgeführt werden soll. Wenn diese Option aktiv ist, wird der Benutzer anschließend aufgefordert, das
Passwort für den Absender-Account einzugeben. Das Passwort wird nicht in der Konfigurationsdatei oder an irgend einem
anderen Ort innerhalb der Anwendung gespeichert.

---
## mail_config.txt
In der Konfigurationsdatei 'mail_config.txt' im Installationsverzeichnis werden die SMTP Verbindungs-Parameter
gesetzt:
### Notwendige Parameter:
**server**: SMTP-Serveradresse des E-Mail-Providers (in Doku des Providers nachschauen)\
**port**: SMTP-Port (in Doku des Providers nachschauen)\
**startTLSEnabled**: true, wenn StartTLS zur Verschlüsselung verwendet wird, sonst false (siehe Doku des E-Mail-Providers)\
**userId**: E-Mail-Adresse des Kontoinhabers (Absenderadresse)

### Optionale Parameter:
**userName**: Name des Kontoinhabers (erscheint als Absender der Nachricht)\
**subject**: Betreffzeile der Email (falls nicht angegeben, erscheint 'Schichtplan' in der Betreffzeile)\
**message**: Eine kurze Nachricht (nicht länger als eine Zeile in der Konfigurationsdatei)\

Kommentare werden mit der Raute (#) eingeleitet und können in einer eigenen Zeile oder hinter einem Schlüssel-Wert-Paar
eingefügt werden.
Leerzeilen sind erlaubt.
Notation der Parameter und des zugeordneten Werts wie folgt: server=mail.gmx.net oder server = mail.gmx.net

Die Anwendung enthält eine Template-Konfigurationsdatei mit weiteren Bespielen und Erläuterungen

---
## shiftplan.xml
Die Parameter für die Erstellung des Schichtplans werden in **shiftplan.xml** festgelegt.\
Das root-Element `shiftplan` hat ein Pflichtattribut `for`, dies ist das Jahr, in welchem der Schichtplan gelten
soll enthaltend. Die optionalen Attribute `start-date` und `end-date` grenzen den Zeitraum innerhalb dieses Jahres
weiter ein; Start- und Enddatum werden mit Jahr-Monat angegeben, z.B. 2023-03.\
Das Element `public-holidays` schließt eine Sequenz von `holiday`-Elementen ein, mit denen gesetzliche Feiertage, die
auf einen Werktag fallen, registriert werden.\
Im Abschnitt `shift-duration` werden folgende Parameter festgelegt:
* `max-home-per-week`: die maximal erlaubte Anzahl von Homeoffice-Tagen pro Woche.
* `max-home-per-month`: die maximal erlaubte Anzahl von Homeoffice-Tagen pro Monat.
* `home-office`: Homeoffice-Zyklus (Anzahl aufeinanderfolgender Tage, an welchen der MA optional von zu Hause arbeiten kann).
* `max-late-shift`: Anzahl der aufeinanderfolgenden Spätschichttage eines MA's. Je nach Aufteilung der Schichten kann
die tatsächliche Anzahl der Tage geringer sein.
* `employee-groups`: Für die Verteilung der Homeoffice-Tage werden die MA's in Gruppen eingeteilt. Jede Homeoffice-Gruppe
hat einen eindeutigen Namen und eine Sequenz von einem oder mehreren Mitarbeitern
* `employee`: Jeder MA wird mit Namen, Nachnamen, E-Mail, einer `boolean`-Flagge für nur Spätschichtteilnahme, der 
Position des MA's bei der Verteilung der Spätschichten (die Reihenfolge hängt von der jeweiligen Homeoffice-Gruppe ab)
und einer Farbe zur Hervorhebung des MA's im Schichtplan konfiguriert.

Weitere Details über die erlaubten XML-Elemente und Attribute, sowie deren Anordnung und die Daten, die sie enthalten 
dürfen, können der Schema-Datei **shiftplan.xsd** entnommen werden.

---
## Lizenz
MIT-License (siehe LICENSE.txt im Installationsverzeichnis)

