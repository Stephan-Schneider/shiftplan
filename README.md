# shiftplan

**shiftplan** ist eine Anwendung zum Erstellen von Spätschicht- und Homeofficeplänen für eine kleinere Abteilung im 
Bereich Spedition / Logistik mit folgenden Anforderungen:

* Jeder Mitarbeiter hat Anspruch auf eine fest definierte Anzahl von Homeoffice-Tagen pro Woche bzw. pro Monat.
* Alle oder eine Teilmenge der Mitarbeiter stehen in abwechselnder Reihenfolge für eine definierte Anzahl von 
Spätschichttagen zur Verfügung. Die Anzahl der Spätschichttage muss innerhalb eines gegebenen Zeitraums gleich auf 
die am Spätschicht-Zyklus teilnehmenden MA's verteilt werden.
* Eine Spätschicht muss in Präsenz im Büro durchgeführt werden, weshalb sich Heimarbeit und Spätschicht ausschließen.
* Mitarbeiter können als nur am Homeoffice-, nur am Spätschicht- oder an beiden Zyklen teilnehmend konfiguriert werden
* Der Schichtplan kann für eine beliebige Zeitspanne innerhalb eines Kalenderjahres erstellt werden.

* Die Anwendung kann lokal als Befehlszeilenprogramm oder als API ausgeführt werden. Für die API existiert ein
  (Web-) Client im Projekt shiftplan_web [https://github.com/Stephan-Schneider/shiftplan_web](https://github.com/Stephan-Schneider/shiftplan_web)

---
## Technische Anforderungen
Die Ausführung der Anwendung erfordert mindestens **Java SE 17** 

---
## Installation und Verwendung

* Entpacken des shiftplan.zip - Archivs in ein beliebiges Verzeichnis
* Nach dem Entpacken enthält das Installationsverzeichnis die Unterordner _'lib'_, _'XML'_ und _'Template'_
* Aufruf der Anwendung durch das Shellskript shiftplan.sh, das sich direkt im Installationsverzeichnis befindet.

### Schichtplan - Konfigurations-Datei
Die Basisdaten eines Schichtplans (Gültigkeitszeitraum, Richtlinien und Mitarbeiter) können in der Datei **shiftplan.xml**
oder **shiftplan.json** hinterlegt werden. 
- Die XML-Datei wird von der Anwendung ausschließlich gelesen, d.h. bei Verwendung der XML-Datei müssen die Daten  
manuell eingegeben werden. Sie ist für die Verwendung bei Ausführung als lokales Programm gedacht.
- Die shiftplan.json wird im Service-Modus von der Anwendung geschrieben und gelesen und wird hauptsächlich
bei der Ausführung als Service verwendet. Sie kann auch lokal verwendet werden, allerdings ist im lokalen Modus nur 
Lesezugriff möglich

### shiftplan.sh
Bei Aufruf des Startskripts 'shiftplan.sh' im lokalen Modus werden folgende Aktionen ausgeführt:
- Einlesen der Konfigurationsdatei 'shiftplan.xml' oder 'shiftplan.json'
- Erstellung des Schichtplans (Spätschicht- und Homeofficeplan) nach Maßgabe der Konfiguration in 'shiftplan.xml'
- Serialisierung des Schichtplans in die Datei **shiftplan_serialized.xml**
- Generierung eines PDFs, das den Schichtplan sowie eine Übersicht über die Zuweisung von Homeoffice-Tagen für jeden MA enthält
- Optional: Versand des PDFs per Email an die in 'shiftplan.xml' hinterlegten Emailadressen
- Nach Erstellung des Plans kann dieser jederzeit durch Angabe der entsprechenden Optionen geändert werden. Spätschichten
  können nur 'en Block' neu vergeben werden (keine Änderung einzelner Tage). Auf Anfrage können bei einer Änderung der 
  Spätschichten auch die Home-Office - Tage neu eingeteilt werden.
- Nach Änderung eines Plans kann ein neues PDF erstellt und an die Mitarbeiter verteilt werden.

Aufruf als Service:
- Der gesamte Funktionsumfang der Anwendung ist auch über einen Web-Client verfügbar. In diesem Fall wird der Server
mit dem Parameter **-S** gestartet. Die Interaktion mit der Anwendung erfolgt anschließend durch den Client.



Aufruf der Hilfefunktion mit dem Parameter **-h**
Das Skript unterstützt folgende Parameter:
* **-x** Pfad zum XML-Ordner. Enthält shiftplan.xml, shiftplan.xsd und shiftplan_serialized.xsd. Obligatorisch, wenn 
der Ordner außerhalb des Installationsverzeichnis liegt
* **-t** Pfad zum Template-Ordner. Enthält shiftplan.ftl. Obligatorisch, wenn der Ordner außerhalb des 
Installationsverzeichnis liegt
* **-g** Pfad zu den von der Anwendung generierten Dateien (shiftplan.json, shiftplan_serialized.xml, Schichtplan.html|.pdf.
* **-e** Pfad zur SMTP-Konfigurationsdatei (bei Emailversand aus dem Programm) - optional
* **-s** sendEmail-Option: nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei mit 
vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus
* **-m** Parameter-String, der die für die Modifizierung eines Schichtplans benötigten Parameter in einem festgelegten Format enthält
* **-j** Pfad zur JSON-Datei, welche die zur Modifikation eines Schichtplans benötigten Parameter enthält. Bei Änderung
eines Schichtplans muss entweder der Parameter -m oder -j angegeben werden
* **-w** Pfad zum öffentlichen Ordner, in dem sich die Webressourcen (HTML-Dateien, in den Browser geladene Skripte etc.) befinden
* **-u** Benutze die shiftplan.json - Konfigurationsdatei anstatt shiftplan.xml bei lokaler Ausführung der Anwendung
* **-l** Erstelle eine Mitarbeiter-Liste mit den jeweiligen Spätschicht-Kalenderwochen bei lokaler Ausführung der Anwendung
* **-S** Mit dieser Option wird ein Webserver gestartet, über welchen Änderungsanforderungen mittels einer Web-Oberfläche
an die Anwendung gesendet werden können - der Web-Client ist nicht in der **shiftplan**-Anwendung enthalten
*  **-H** Host-Adresse, an die der Server-Socket gebunden wird (localhost|127.0.0.1 oder 0.0.0.0)
* **-P** Port, an dem der Server gestartet wird. Der Default-Port ist _8080_

#### Aufruf-Beispiele
Aufruf des Skripts immer aus dem Installationsverzeichnis der Anwendung:
````bash
cd /path/to/shiftplan_installdir
````
Anschließend:
* Erstellen eines neuen Schichtplans:
```bash
# Erstellen eines Schichtplans, lokaler Aufruf, Default-Locations, Email-Versand
# Für das XML- und Template-Verzeichnis sowie für den Pfad zur Email-Konfiguration können die in shiftplan.sh 
# eingetragenen Default-Werte genommen werden.
# Mit dem Parameter -u kann eine existierende shiftplan.json Datei als Schichtplan-Konfigurationsdatei verwendet werden.
./shiftplan.sh -g /home/$user/dir -s -m "CREATE"
```

```bash
# Erstellen einer Mitarbeiterliste mit Auflistung der Spätschichten jedes Mitarbeiters (druckt die Liste auf der Console
# aus und erstellt zusätzlich eine Datei im Verzeichnis generated_data
./shiftplan.sh -l -x /home/$user/xmlPath -g /home/$user/generated_data_dir
```

* Ändern eines bestehenden Schichtplans:
```bash
# Ändern eines Schichtplans, lokaler Aufruf, Default-Locations, Email-Versand#
./shiftplan.sh -m "SWAP,true,ID-1,35,ID-4,37" -x /home/$user/xmlPath -g /home/$user/generated_data -s
```

* Starten des Webservers
```bash
# Starten des Webservers Default-Locations
# Die Übergabe der Änderungsparameter (SWAP,ID_MA1 etc.) und das Triggern des Emailversands erfolgen über die HTTP-Rest URLs
./shiftplan.sh -S -H localhost -P 8080 -w /home/$user/webResources -g /home/$user/generated_data -x /home/$user/xmlPath > /var/log/shiftplan_log/undertow.log 2>&1 &
```

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
* `max-successive-ho-days`: Maximal erlaubte Anzahl von aufeinanderfolgenden Home-Office Tagen. Der Wert dieses Parameters
wird normalerweise unter der maximalen Anzahl von HO-Tagen pro Woche liegen, d.h. die erlaubte Anzahl von HO-Tagen
wird innerhalb einer Woche verteilt und nicht als fortlaufender, ununterbrochener Block genommen. (Beispiel: einem MA
stehen max. 3 Tage pro Woche zu, die maximale Anzahl an fortlaufenden HO-Tagen, ebenso wie die Sperrfrist (siehe 
`min-distance-between-ho-blocks`), ist aber auf 2 Tage begrenzt. Er kann daher z.B. Montag, Dienstag eingeteilt werden,
  dann aber erst wieder z.B. am Freitag der gleichen Woche).
* `min-distance-between-ho-blocks`: Mindestanzahl von Tagen zwischen zwei Home-Office-Blöcken. Ein MA ist z.B. Montag
und Dienstag ins HO eingeteilt, hat damit die maximale Anzahl fortlaufender HO-Tage erreicht und kann dann erst nach 
einer Sperrfrist von zwei Tagen (also Freitag) erneut eingeteilt werden, um die max. erlaubte Gesamtanzahl von drei Tagen 
pro Woche zu erreichen.

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

