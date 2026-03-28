#!/bin/bash

echo "copy_webfiles wird ausgeführt"
cp -R /home/stephan/Projekte/Konzepte/shiftplan/dist /home/stephan/Projekte/Idea/Java/Shiftplan/target
mv target/shiftplan-2.0-SNAPSHOT-jar-with-dependencies.jar target/shiftplan.jar