FROM eclipse-temurin:17-jre-alpine

RUN mkdir -p /opt/shiftplan
# Die Basis-URL für den Web-Client ist in der Datei /web/runtime-config.json wie folgt notiert:
# >> { "BASE_URL": "http://localhost:8080/api/shiftplan"} <<
# Falls die Anwendung hinter einem Reverse-Proxy läuft, nur '/api/shiftpplan' angeben
# Falls die URL geändert werden muss, kann die Json-Datei mit dem Befehl
# `cp /pfad/zu/runtime-config.json container:/web/runtime-config.json` ersetzt werden.
RUN mkdir /web
RUN mkdir /generated_data
RUN mkdir /generated_pages
RUN mkdir /secrets
RUN ln -sf /generated_pages /web/generated

ENV APP_PORT=8080
ENV WEB_DIR=/web
ENV GEN_DIR=/generated_data

# Der User-Name wird durch die Umgebungsvariabel 'SHIFTPLAN_USER' gesetzt.
# Das Passwort kann durch eine Umgebungsvariabel 'SHIFTPLAN_PASSWORD' gesetzt werden oder
# in einer Passwortdatei 'password_file' notiert werden. Das Verzeichnis auf dem Host, das
# diese Datei enthält muss dann in das /secrets - Verzeichnis des Containers gemounted werden.
ENV SHIFTPLAN_USER=Schicht-Exp

COPY target/shiftplan.jar /opt/shiftplan
COPY target/dist /web/

EXPOSE 8080

CMD ["sh", "-c", "exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.net.preferIPv4Stack=true -jar /opt/shiftplan/shiftplan.jar -S -H 0.0.0.0 -P ${APP_PORT} -w ${WEB_DIR} -g ${GEN_DIR}"]