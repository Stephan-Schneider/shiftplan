package shiftplan.publish;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

public class SMTPConfig {

    /*
        # SMTP - Verbindungsparameter

        # folgende Parameter sind erforderlich:
        # server: SMTP-Serveradresse des E-Mail-Providers (in Doku des Providers nachschauen)
        # port: SMTP-Port (in Doku des Providers nachschauen)
        # startTLSEnabled: true, wenn StartTLS zur Verschlüsselung verwendet wird, sonst false (siehe Doku des E-Mail-Providers)
        # userId: E-Mail-Adresse des Kontoinhabers (Absenderadresse)

        # Optionale Parameter:
        # userName: Name des Kontoinhabers (erscheint als Absender der Nachricht)
        # subject: Betreffzeile der Email (falls nicht angegeben, erscheint 'Schichtplan' in der Betreffzeile)
        # message: Eine kurze Nachricht (nicht länger als eine Zeile in dieser Konfigurationsdatei)

        server=mail.gmx.net
        port = 587 # Standardports: SSL: 465 // StartTLS: 587
        startTLSEnabled = true
        userId = stephan.geert.schneider@gmx.de
        userName = Stephan Schneider
        subject = Schichtplan für 2023
        message = Hallo zusammen,<br><br>im Anhang ist der neue Schichtplan.
     */

    private static final Logger logger = LogManager.getLogger(SMTPConfig.class);

    public static SMTPConfig getConfigParams(String... path) throws IOException {
        logger.info("Die Email-Konfigurationsparameter werden ausgelesen");
        logger.debug("path: {}", Arrays.toString(path));
        SMTPConfig smtpConfig = new SMTPConfig();
        if (path == null || path.length == 0) {
            // Die Konfigurationsdatei im Ordner 'resources' während der Entwicklungsphase oder im JAR-Archiv wird
            // verwendet
            smtpConfig.readConfigFile();
        } else {
            // Die Konfigurationsdatei im Installationsverzeichnis oder der explizit bei Aufruf des Programms
            // als Argument angegebene Pfad wird verwendet
            smtpConfig.readConfigFile(path[0]);
        }
        return smtpConfig;
    }

    private String smtpServer;
    private int smtpPort;
    private boolean startTLSEnabled;
    private String userId;
    private String userName;

    private String subject;
    private String message;

    private SMTPConfig() {}

    public String getSmtpServer() {
        return smtpServer;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public boolean isStartTLSEnabled() {
        return startTLSEnabled;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName == null ? "" : userName;
    }

    public String getSubject() {
        return subject == null ? "Schichtplan" : subject;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    private void readConfigFile() {
        InputStream in = SMTPConfig.class.getClassLoader().getResourceAsStream("mail_config.txt");
        assert in != null;

        try (Scanner scanner = new Scanner(in)) {
            processInput(scanner);
        }
    }

     private void readConfigFile(String pathToConfigFile) throws IllegalArgumentException, IOException {
        Path path = Path.of(pathToConfigFile);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Kein gültiger Pfad zur Email-Konfigurationsdatei: " + pathToConfigFile);
        }

        try (Scanner scanner = new Scanner(path)) {
            processInput(scanner);
        }
    }

    private void processInput(Scanner scanner) {
        assert scanner != null;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isBlank() || line.startsWith("#")) continue;

            String[] keyValuePair = line.split("\\s*=\\s*");
            if (keyValuePair.length == 2) {
                String key = keyValuePair[0].strip();
                String value = keyValuePair[1].strip();

                String[] valueAndComments = value.split("\\s+#\\s*");
                value = valueAndComments[0];
                logger.debug("Key: {} / Value: {}", key, value);

                switch (key) {
                    case "server" -> smtpServer = value;
                    case "port" -> smtpPort = Integer.parseInt(value);
                    case "startTLSEnabled" -> startTLSEnabled = Boolean.parseBoolean(value);
                    case "userId" -> userId = value;
                    case "userName" -> userName = value;
                    case "subject" -> subject = value;
                    case "message" -> message = value;
                    default -> throw new IllegalArgumentException("Ungültiger Schlüssen in mail_config.txt: " + key);
                }
            }
        }
    }
}
