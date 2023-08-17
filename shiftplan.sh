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

# Pfad zum Verzeichnis, in welchem sich shiftplan.xml und shiftplan.xsd befinden - diese beiden Dateien müssen
# sich immer in gleichen Ordner befinden
xmlPath=${install_dir}/XML

# Pfad zum Template-Verzeichnis - enthält das FTL-Template 'shiftplan.ftl'
templatePath=${install_dir}/Template

# Pfad zum Verzeichnis, in dem die generierte Schichtplan-Datei (temporär) als PDF gespeichert wird
outDir=""

# Pfad zur SMTP-Konfigurationsdatei (enthält SMTP Parameter wie den SMTP-Server des Email-Providers)
configPath=${install_dir}/mail_config.txt

# Indikator, ob der Schichtplan, direkt nach seiner Erstellung, per Email an die einzelnen Mitarbeiter versendet
# werden soll
sendEmail=false

# Email-Passwort. Bei lokalem Aufruf des Programms über die Shell sollte dieser Parameter NICHT angegeben werden,
# da das Passwort in Klartext erscheint. Bei lokalem Aufruf den interaktiven Modus verwenden!!!
smtpPassword=""

# Indikator, ob das Email-Passwort interaktiv auf der Kommandozeile abgefragt werden soll. Diese Option sollte nur
# angegeben werden, wenn das Programm lokal über die Bash-Shell aufgerufen wird. Wird das Programm durch einen
# entfernten SSH-Client aufgerufen, sollte das Passwort ohne interaktive Abfrage durch das shiftplan.sh-Skript vom Client
# übermittelt werden. Im interaktiven Modus wird das Passwort bei der Eingabe maskiert.
interactive=false

# Pfad zur XML-Schema - Datei, die zur Validierung von shiftplan_serialized.xml verwendet wird
shiftPlanCopySchemaPath=/home/stephan/Projekte/Web

# Pfad zur Kopie eines Schichtplans in XML-Format - die XML-Datei hat immer den Namen 'shiftplan_serialized.xml'
shiftPlanCopyXMLFile=/home/stephan/Projekte/Web/shiftplan_serialized.xml

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

# shellcheck disable=SC1009
help() {
  echo "Skript zum Starten der shiftplan - App"
  echo
  echo "Aufruf: shiftplan.sh [-h|-x|-t|-o|-c|-s|-p|-i|-v|-d|-m|-j]"
  echo "Optionen:"
  echo "-h  Druckt diese Hilfenachricht"
  echo "-x  Pfad zum XML-Ordner. Enthält shiftplan.xml und shiftplan.xsd. Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt"
  echo "-t  Pfad zum Template-Ordner. Enthält shiftplan.ftl. Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt"
  echo "-o  Pfad zum Speicherort (Verzeichnis) des erstellten Schichtplans. Falls nicht angeben, wird die generierte Datei im Standardverzeichnis für Temp-Dateien des jeweiligen Betriebssystems abgelegt"
  echo "-c  Pfad zur Konfigurationsdatei (bei Emailversand aus dem Programm) - optional"
  echo "-s  sendEmail-Option: nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei mit vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus"
  echo "-p  Email-Passwort. Dieser Parameter ist für Passwortübergabe durch einen entfernten SSH-Client gedacht. Bei lokalem Aufruf den interaktiven Modus benutzen!"
  echo "-1  Interaktive Passwortabfrage - die Übergabe des Email-Passworts sollte bei lokalem Aufruf des Programms interaktiv erfolgen"
  echo "-v  Pfad zur Schema-Datei, mit welcher der serialisierte Schichtplan beim Einlesen des XMLs validiert wird"
  echo "-d  Pfad zur XML-Datei, welche die Kopie des Schichtplans enthält (shiftplan_serialized.xml)"
  echo "-m  Parameter-String, der die für die Modifizierung eines Schichtplans benötigten Parameter in einem festgelegten Format enthält"
  echo "-j  Pfad zur JSON-Datei, welche die zur Modifikation eines Schichtplans benötigten Parameter enthält"
}

while getopts "hx:t:o:c:sp:iv:d:m:j:" option 2>/dev/null; do
  case $option in
  h) # Hilfe-Text ausgeben
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
    sendEmail=true;;
  p) # SMTP-Passwort
    smtpPassword=$OPTARG;;
  i) # Interaktive Email-Passwortabfrage bei lokalem Aufruf des Programms
    interactive=true;;
  v) # Pfad zum Verzeichnis, welches die Schema-Datei für die XML-Schichtplankopie enthält
    shiftPlanCopySchemaPath=$OPTARG;;
  d) # Pfad zur XML-Datei, die den Schichtplan in XML-Format enthält
    shiftPlanCopyXMLFile=$OPTARG;;
  m) # Operations-Parameter für das Ändern eines Schichtplans (Schichttausch)
    swapData=$OPTARG;;
  j) # Pfad zur JSON-Datei mit den Operations-Parametern für das Ändern eines Schichtplans (Schichttausch)
    swapDataJSON=$OPTARG;;
  *) # Ungültige Option
    echo "Ungültige Option - Skript wird beendet"
    exit ;;
  esac

done

echo "Starten der shiftplan - Anwendung aus Verzeichnis ${install_dir} ..."

if [ "$sendEmail" = true ] && [ "$interactive" = true ]; then
    echo "Bitte das Passwort für den Email-Account des Absenders eingeben:"
    read -s -r smtpPassword
    # echo "$smtpPassword"
fi

if [ "$sendEmail" = true ]; then
  java --module-path shiftplan-2.0-SNAPSHOT.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner -x "$xmlPath" -t "$templatePath" \
  -o "$outDir" -c "$configPath" -p "$smtpPassword" -s -v "$shiftPlanCopySchemaPath" -d "$shiftPlanCopyXMLFile" \
  -m "$swapData" -j "$swapDataJSON"
else
  java --module-path shiftplan-2.0-SNAPSHOT.jar:lib -m shiftplan/shiftplan.ShiftPlanRunner -x "$xmlPath" -t "$templatePath" \
  -o "$outDir" -v "$shiftPlanCopySchemaPath" -d "$shiftPlanCopyXMLFile" -m "$swapData" -j "$swapDataJSON"
fi
