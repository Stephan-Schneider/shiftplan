# shiftplan

**shiftplan** ist eine Anwendung zum Erstellen von Spätschicht- und Homeofficeplänen für eine kleine Abteilung mit 
folgenden Anforderungen:

* Jeder Mitarbeiter hat Anspruch auf eine fest definierte Anzahl von Homeoffice-Tagen pro Woche bzw. pro Monat.
* Alle oder eine Teilmenge der Mitarbeiter stehen in abwechselnder Reihenfolge für eine definierte Anzahl von 
Spätschichttagen zur Verfügung. Die Anzahl der Spätschichttage muss innerhalb eines gegebenen Zeitraums gleich auf 
die am Spätschicht-Zyklus teilnehmenden MA's verteilt werden.
* Eine Spätschicht muss in Präsenz im Büro durchgeführt werden, weshalb sich Heimarbeit und Spätschicht ausschließen.
* Mitarbeiter können als nur am Homeoffice-, nur am Spätschicht- oder an beiden Zyklen teilnehmend konfiguriert werden
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
Bei Aufruf des Startskripts 'shiftplan.sh' werden folgende Aktionen ausgeführt:
- Einlesen der Konfigurationsdatei 'shiftplan.xml'
- Erstellung des Schichtplans (Spätschicht- und Homeofficeplan) nach Maßgabe der Konfiguration in 'shiftplan.xml'
- Generierung eines PDFs, das den Schichtplan sowie eine Übersicht über die Zuweisung von Homeoffice-Tagen für jeden MA enthält
- Optional: Versand des PDFs per Email an die in 'shiftplan.xml' hinterlegten Emailadressen


Aufruf der Hilfefunktion mit dem Parameter **-h**
Das Skript unterstützt folgende Parameter:
* **-x:** Pfad zum Ordner, der die Konfigurationsdatei shiftplan.xml und die Schema-Datei shiftplan.xsd enthält. In der
Voreinstellung ist dies der Ordner \<Installationsverzeichis\>/XML. Die XML- und Schema-Datei können jedoch in 
ein beliebiges, anderes Verzeichnis verschoben werden, der Pfad ist in diesem Fall mit der Option **-x** entsprechend 
anzugeben. Beide Dateien müssen sich allerdings immer im gleichen Verzeichnis befinden.
* **-t:** Pfad zum Ordner, der das Template shiftplan.ftl enthält. In der Voreinstellung ist das der Ordner
\<Installationsverzeichnis\>/Template. Falls das Template in ein anderes Verzeichnis verschoben wird, ist das neue
Verzeichnis mit der Option **-t** anzugeben.
* **-o** Pfad zum Speicherort (Verzeichnis) des generierten Schichtplans. Falls nicht angegeben, wird eine
temporäre Datei im Standardverzeichnis für Temp-Dateien des jeweiligen Betriebssystems angelegt.
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

Die Anwendung enthält eine Template-Konfigurationsdatei mit weiteren Beispielen und Erläuterungen

---
## shiftplan.xml
Die Parameter für die Erstellung des Schichtplans werden in **shiftplan.xml** festgelegt.\
Das root-Element `shiftplan` hat ein Pflichtattribut `for`, welches das Jahr, in welchem der Schichtplan gelten
soll, enthält. Die optionalen Attribute `start-date` und `end-date` grenzen den Zeitraum innerhalb dieses Jahres
weiter ein; Start- und Enddatum werden mit Jahr-Monat angegeben, z.B. 2023-03.\
Das Element `public-holidays` schließt eine Sequenz von `holiday`-Elementen ein, mit denen gesetzliche Feiertage, die
auf einen Werktag fallen, registriert werden.\
Im Abschnitt `shift-policy` werden folgende Parameter festgelegt:
* `late-shift-period`: Anzahl der aufeinanderfolgenden Spätschichttage eines MA's.
* `no-lateshift-on`: Wochentage, an denen keine Spätschicht stattfindet. Der XML-Tag kann maximal 5 x enthalten sein
  (für alle Werktage von Mo. bis Fr.). Die Wochentage müssen in Großbuchstaben auf Englisch angegeben werden (z.B. MONDAY)
* `max-home-per-month`: die maximal erlaubte Anzahl von Homeoffice-Tagen pro Monat.
* `ho-credits-per-employee`: die maximal erlaubte Anzahl von Homeoffice-Tagen pro Woche für einen MA.
* `max-ho-slots-per-day`: Maximale Anzahl von MA's, die an einem Tag von zu Hause arbeiten.

Es folgt der Konfigurationsabschnitt für die einzelnen Mitarbeiter:
* `employees`: Liste der Mitarbeiter/innen der Abteilung.
* `employee`: Im Pflicht-Attribut `id` wird die eindeutige ID des MA's angegeben. `employee` enthält die nachfolgend 
aufgelisteten Kindelemente:

Obligatorische XML-Tags:
* `name`: Vorname
* `lastname`: Nachname
* `participation` Festlegung des Teilnahme-Modus: 'HO': nur Homeoffice, 'LS': nur Spätschicht,
'HO_LS': Homeoffice und Spätschicht.

Optionale XML-Tags:
* `email` (optional): Email-Adresse, an welche der generierte Schichtplan automatisch gesendet wird.
* `color`: (optional) Hervorhebungsfarbe im Schichtplan zur besseren Übersichtlichkeit des Plans.
* `backups`: (optional) enthält den XML-Pflichttag `backup` mit dem Attribut 'idref': durch die Angabe der ID eines oder
 mehrerer Mitarbeiter können diese als Backup konfiguriert werden. Die Anzahl der Backups ist theoretisch unbegrenzt.

Weitere Details über die erlaubten XML-Elemente und Attribute, sowie deren Anordnung und die Daten, die sie enthalten 
dürfen, können der Schema-Datei **shiftplan.xsd** entnommen werden, die zur Validierung der shiftplan.xml-Datei verwendet 
wird.

---
## Lizenz
MIT-License (siehe LICENSE.txt im Installationsverzeichnis)

