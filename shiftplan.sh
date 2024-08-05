#!/bin/bash

# Skript zum Starten der shiftplan - App

# Falls ein Symlink (z.B. in ~/bin) auf das Startskript gesetzt wurde, wird mit readlink zuerst der
# reale Ort des Startskripts im Dateisystem ermittelt.
# Anschließend wird das Elternverzeichnis des Startskripts als Installationsverzeichnis gesetzt.
if [ -L "${0}" ]; then
    script_path=$(readlink "${0}")
    install_dir=$(dirname "$script_path")
else
    install_dir=$(dirname "${0}")
fi

echo "Die Anwendung befindet sich im Verzeichnis: $install_dir"

# Wechseln ins Installationsverzeichnis - der Aufruf des Programms muss vom Installationsverzeichnis aus erfolgen.
cd "$install_dir" || exit
echo "Aktuelles Arbeitsverzeichnis: ${PWD}"


#############################
# Voreingestellte Parameter #
#############################

# Pfad zum Verzeichnis, in welchem sich shiftplan.xml, shiftplan.xsd und shiftplan_serialized.sxd befinden - diese
# Dateien müssen sich immer in gleichen Ordner befinden
xmlPath=${install_dir}/XML

# Pfad zum Template-Verzeichnis - enthält das FTL-Template 'shiftplan.ftl'
templatePath=${install_dir}/Template

# Pfad zum Verzeichnis, in dem die von der Anwendung generierten Dateien gespeichert werden ((shiftplan.json,
# shiftplan_serialized.xml, Schichtplan.html|.pdf.)
generated_data=""

# Pfad zur SMTP-Konfigurationsdatei (enthält SMTP Parameter wie den SMTP-Server des Email-Providers)
mailConfigPath=${install_dir}/mail_config.txt

# Indikator, ob der Schichtplan, direkt nach seiner Erstellung, per Email an die einzelnen Mitarbeiter versendet
# werden soll
sendEmail=false

# Email-Passwort. Bei lokalem Aufruf des Programms über die Shell sollte dieser Parameter NICHT angegeben werden,
# da das Passwort in Klartext erscheint. Bei lokalem Aufruf den interaktiven Modus verwenden!!!
smtpPassword=""

# Die für die Modifizierung eines Schichtplans (SWAP, REPLACE, CREATE) erforderlichen OP-Parameter als CLI-Args
# Der Parameter-String muss die einzelnen Parameter in einer festgelegten Reihenfolge enthalten:
# 1.) REPLACE: 'REPLACE, true|false, employee1-ID, cwIndex1, employee2-ID'
# 2.) SWAP: 'SWAP, true|false, employee1-ID, cwIndex1, employee2-ID, cwIndex2'
# 3.) CREATE: 'CREATE'
swapData=""

# Anstatt die OP-Parameter als CLI-Args zu übergeben, können diese auch in einer JSON-Datei (swap_params.json)
# notiert werden. Der Pfad zu swap_params.json wird in 'swapDataJSON' gespeichert.
# Die Verwendung der swap_params.json-Datei ist nur bei einem lokalen Aufruf des Skripts sinnvoll. Wenn das Skript/Programm
# durch einen SSH-Client aufgerufen wird, sollten die Parameter als CLI-Args übergeben werden
swapDataJSON=${install_dir}/swap_data.json

webResourcesPath=""

createStaffList=false

useJsonShiftplanConfig=false

# Wird <server> auf den Wert <true> gesetzt, startet die Anwendung einen Webserver zur Ausführung der Anwendung als
# Web-Service
server=false

# Host-Adresse, an die der Server-Socket gebunden wird (localhost|127.0.0.1 oder 0.0.0.0)
host=localhost

# Port, an welchen der Server gestartet wird.
port=8080

# shellcheck disable=SC1009
help() {
  echo "Skript zum Starten der Schichtplan - App"
  echo
  echo "Aufruf: shiftplan.sh [-h|-x|-t|-g|-e|-s|-m|-j|-w|-u|-l-S|-H|-P]"
  echo "Optionen:"
  echo "-h  Druckt diese Hilfenachricht"
  echo "-x  Pfad zum XML-Ordner. Enthält shiftplan.xml, shiftplan.xsd und shiftplan_serialized.xsd. Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt"
  echo "-t  Pfad zum Template-Ordner. Enthält shiftplan.ftl. Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt"
  echo "-g  Pfad zu den von der Anwendung generierten Dateien (shiftplan.json, shiftplan_serialized.xml, Schichtplan.html|.pdf."
  echo "-e  Pfad zur SMTP-Konfigurationsdatei (bei Emailversand aus dem Programm) - optional"
  echo "-s  sendEmail-Option: nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei mit vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus"
  echo "-m  Parameter-String, der die für die Modifizierung eines Schichtplans benötigten Parameter in einem festgelegten Format enthält"
  echo "-j  Pfad zur JSON-Datei, welche die zur Modifikation eines Schichtplans benötigten Parameter enthält"
  echo "-w  Pfad zum öffentlichen Ordner, in dem sich die Webressourcen (HTML-Dateien, in den Browser geladene Skripte etc.) befinden"
  echo "-u  Benutze die shiftplan.json - Konfigurationsdatei anstatt shiftplan.xml bei lokaler Ausführung der Anwendung"
  echo "-l  Erstelle eine Mitarbeiter-Liste mit den jeweiligen Spätschicht-Kalenderwochen bei lokaler Ausführung der Anwendung"
  echo "-S  Startet einen Webserver für den Remote-Zugriff zum ändern eines existierenden Schichtplan. Ohne den Parameter '-S' läuft die Verbindung über SSH"
  echo "-H  Host-Adresse, an die der Server-Socket gebunden wird (localhost|127.0.0.1 oder 0.0.0.0)"
  echo "-P  Port, an welchem der Webserver gestartet wird"

}

while getopts "hx:t:g:e:sm:j:w:ulSH:P:" option 2>/dev/null; do
  case $option in
  h) # Hilfe-Text ausgeben
    help
    exit ;;
  x) # XML-Pfad
    xmlPath=$OPTARG;;
  t) # Template-Pfad
    templatePath=$OPTARG;;
  g) # Verzeichnis für die generierten Dateien
    generated_data=$OPTARG;;
  e) # Pfad zur SMTP-Konfigurationsdatei
    mailConfigPath=$OPTARG;;
  s) # Emailversand aktivieren (Ja, falls Parameter angegeben)
    sendEmail=true;;
  m) # Operations-Parameter für das Ändern eines Schichtplans (Schichttausch)
    swapData=$OPTARG;;
  j) # Pfad zur JSON-Datei mit den Operations-Parametern für das Ändern eines Schichtplans (Schichttausch)
    swapDataJSON=$OPTARG;;
  w) # Pfad zum public-Ordner (Webanwendung)
    webResourcesPath=$OPTARG;;
  u) # Benutze shiftplan.json anstatt shiftplan.xml (lokale Ausführung)
    useJsonShiftplanConfig=true;;
  l) # Erstelle eine Mitarbeiter-Liste (lokale Ausführung)
    createStaffList=true;;
  S) # Starten des Web-Servers
    server=true;;
  H) # Host-Parameter
    host=$OPTARG;;
  P) # Http-Port
    port=$OPTARG;;
  *) # Ungültige Option
    echo "Ungültige Option - Skript wird beendet"
    exit ;;
  esac

done

echo "Starten der shiftplan - Anwendung aus Verzeichnis ${install_dir} ..."

if [ "$createStaffList" = true ]; then
    java --module-path shiftplan-2.0-SNAPSHOT.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner -l -x "$xmlPath" \
    -g "$generated_data"
    exit 0 # Skript stoppen - keine weitere Aktion erforderlich oder sinnvoll
fi

if [ "$sendEmail" = true ]; then
    echo "Bitte das Passwort für den Email-Account des Absenders eingeben:"
    read -s -r smtpPassword
    # echo "$smtpPassword"
fi

if [ "$server" = true ]; then # Ausführung als Web-Service
    java --module-path shiftplan-2.0-SNAPSHOT.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner \
    -S -H "$host" -P "$port" -w "$webResourcesPath" -x "$xmlPath" -t "$templatePath" -g "$generated_data" \
     -e "$mailConfigPath" -p "$smtpPassword"
elif [ "$sendEmail" = true ]; then # Ausführung als Befehlszeilen-Programm mit Emailversand
  java --module-path shiftplan-2.0-SNAPSHOT.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner \
  -x "$xmlPath" -t "$templatePath" -g "$generated_data" -s -e "$mailConfigPath" -p "$smtpPassword" -m "$swapData" \
  -j "$swapDataJSON" -u "$useJsonShiftplanConfig"
else # Ausführung als Befehlszeilen-Programm ohne Emailversand
  java --module-path shiftplan-2.0-SNAPSHOT.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner \
   -x "$xmlPath" -t "$templatePath" -g "$generated_data" -m "$swapData" -j "$swapDataJSON" -u "$useJsonShiftplanConfig"
fi
