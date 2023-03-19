#!/bin/bash

# Skript zum Starten der shiftplan - App

# Falls ein Symlink (z.B. in ~/bin) auf das Startskript gesetzt wurde, wird mit readlink zuerst der
# reale Ort des Startskripts im Dateisystem ermittelt.
# Anschließend wird das Elternverzeichnis des Startskripts als Installationsverzeichnis gesetzt.
if [ -L "${0}" ]; then
    script_path=$(readlink "${0}")
    install_dir=$(dirname "$script_path")
else
    install_dir=$(dirname "$0")
fi

echo "Die Anwendung befindet sich im Verzeichnis: $install_dir"

# Wechseln ins Installationsverzeichnis - der Aufruf des Programms muss vom Installationsverzeichnis aus erfolgen.
cd "$install_dir" || exit
echo "Aktuelles Arbeitsverzeichnis: ${PWD}"


#############################
# Voreingestellte Parameter #
#############################

# Pfad zum Verzeichnis, in welchem sich shiftplan.xml und shiftplan.xsd befinden - diese beiden Dateien müssen
# sich immer in gleichen Ordner befinden
xmlPath=${install_dir}/XML

# Pfad zum Template-Verzeichnis - enthält das FTL-Template 'shiftplan.ftl
templatePath=${install_dir}/Template

# Pfad zum Verzeichnis, in dem die generierte Schichtplan-Datei (temporär) gespeichert wird
outDir=""

# Pfad zur SMTP-Konfigurationsdatei (enthält SMTP Parameter wie den SMTP-Server des Email-Providers)
configPath=${install_dir}/mail_config.txt

# Indikator, ob der Schichtplan, direkt nach seiner Erstellung, per Email an die einzelnen Mitarbeiter versendet
# werden soll
sendEmail=false

# shellcheck disable=SC1009
help() {
  echo "Skript zum Starten der shiftplan - App"
  echo
  echo "Aufruf: shiftplan.sh [-h|-x|-t|-o|-c|-s]"
  echo "Optionen:"
  echo "-h  Druckt diese Hilfenachricht"
  echo "-x  Pfad zum XML-Ordner. Enthält shiftplan.xml und shiftplan.xsd. Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt"
  echo "-t  Pfad zum Template-Ordner. Enthält shiftplan.ftl. Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt"
  echo "-o  Pfad zum Speicherort (Verzeichnis) des erstellten Schichtplans. Falls nicht angeben, wird die generierte Datei im Standardverzeichnis für Temp-Dateien des jeweiligen Betriebssystems abgelegt"
  echo "-c  Pfad zur Konfigurationsdatei (bei Emailversand aus dem Programm) - optional"
  echo "-s  sendEmail-Option: nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei mit vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus"
}

while getopts "hx:t:o:c:s" option 2>/dev/null; do
  case $option in
  h) # Help-Text drucken
    help
    exit ;;
  x) # XML-Pfad
    xmlPath=$OPTARG;;
  t) # Template-Pfad
    templatePath=$OPTARG;;
  o) # Verzeichnis für die generierte Schichtplan-Datei
    outDir=$OPTARG;;
  c) # Pfad zur SMTP-Konfigurationsdatei
    configPath=$OPTARG;;
  s) # Emailversand aktivieren (Ja, falls Parameter angegeben)
    sendEmail=true ;;
  *) # Ungültige Option
    echo "Ungültige Option - Skript wird beendet"
    exit ;;
  esac

done

echo "Starten der shiftplan - Anwendung aus Verzeichnis ${install_dir} ..."

if [ "$sendEmail" = true ]; then
  echo "Bitte das Passwort für den Email-Account des Absenders eingeben:"
  read -s -r smtpPassword
  # echo "$smtpPassword"
  java --module-path shiftplan-1.0.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner -x "$xmlPath" -t "$templatePath" -o "$outDir" -c "$configPath" -p "$smtpPassword" -s
else
  java --module-path shiftplan-1.0.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner -x "$xmlPath" -t "$templatePath" -o "$outDir"
fi
