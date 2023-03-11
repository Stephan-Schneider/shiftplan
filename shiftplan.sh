#!/bin/bash

# Skript zum Starten der shiftplan - App

# Installationsverzeichnis - der Aufruf des Programms muss vom Installationsverzeichnis aus erfolgen.
install_dir=${PWD}


#############################
# Voreingestellte Parameter #
#############################

# Pfad zum Verzeichnis, in welchem sich shiftplan.xml und shiftplan.xsd befinden - diese beiden Dateien müssen
# sich immer in gleichen Ordner befinden
xmlPath=${install_dir}/XML

# Pfad zum Template-Verzeichnis - enthält das FTL-Template 'shiftplan.ftl
templatePath=${install_dir}/Template

# Pfad zur SMTP-Konfigurationsdatei (enthält SMTP Parameter wie den SMTP-Server des Email-Providers)
configPath=${install_dir}/config.txt

# Passwort des Absender-Kontos für den Emailversand
smtpPassword=""

# Indikator, ob der Schichtplan, direkt nach seiner Erstellung, per Email an die einzelnen Mitarbeiter versendet
# werden soll
sendEmail=false

# shellcheck disable=SC1009
help() {
  echo "Skript zum Starten der shiftplan - App"
  echo
  echo "Aufruf: shiftplan.sh [-h|-x|-t|-c|-p|-s]"
  echo "Optionen:"
  echo "-h  Druckt diese Hilfenachricht"
  echo "-x  Pfad zum XML-Ordner. Enthält shiftplan.xml und shiftplan.xsd. Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt"
  echo "-t  Pfad zum Template-Ordner. Enthält shiftplan.ftl. Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt"
  echo "-c  Pfad zur Konfigurationsdatei (bei Emailversand aus dem Programm) - optional"
  echo "-p  SMTP-Passwort (bei Emailversand aus dem Programm) - optional. Obligatorisch nur, wenn Emailversand aktiviert"
  echo "-s  sendEmail-Option: nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei mit vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus"
}

while getopts "hx:t:c:p:s" option 2>/dev/null; do
  case $option in
  h) # Help-Text drucken
    help
    exit ;;
  x) # XML-Pfad
    xmlPath=$OPTARG;;
  t) # Template-Pfad
    templatePath=$OPTARG;;
  c) # Pfad zur SMTP-Konfigurationsdatei
    configPath=$OPTARG;;
  p) # Passwort
    smtpPassword=$OPTARG;;
  s) # Emailversand aktivieren (Ja, falls Parameter angegeben)
    sendEmail=true ;;
  *) # Ungültige Option
    echo "Ungültige Option - Skript wird beendet"
    exit ;;
  esac

done

echo "Starten der shiftplan - Anwendung aus Verzeichnis ${install_dir} ..."

if [ "$sendEmail" = true ]; then
  java --module-path shiftplan-1.0-SNAPSHOT.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner -x "$xmlPath" -t "$templatePath" -c "$configPath" -p "$smtpPassword" -s
else
  java --module-path shiftplan-1.0-SNAPSHOT.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner -x "$xmlPath" -t "$templatePath"
fi
