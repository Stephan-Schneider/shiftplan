package shiftplan.publish;

import org.apache.commons.mail.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.users.Employee;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class EmailDispatch {

    private static final Logger logger = LogManager.getLogger(EmailDispatch.class);
    private final SMTPConfig config;

    public EmailDispatch(String configPath) throws IOException {
        if (configPath == null) {
            // Die Konfigurationsdatei im Ordner 'resources' während der Entwicklungsphase oder im JAR-Archiv wird
            // verwendet
            config = SMTPConfig.getConfigParams();
        } else {
            // Die Konfigurationsdatei im Installationsverzeichnis oder der explizit bei Aufruf des Programms
            // als Argument angegebene Pfad wird verwendet
            config = SMTPConfig.getConfigParams(configPath);
        }
    }

    public void sendMail(List<Employee> employees, Path attachment, String password) throws EmailException {
        logger.info("Der erstellte Schichtplan wird per Email versendet");
        MultiPartEmail email = new HtmlEmail();

        email.setHostName(config.getSmtpServer());
        email.setSmtpPort(config.getSmtpPort());
        email.setStartTLSEnabled(config.isStartTLSEnabled());
        email.setAuthenticator(new DefaultAuthenticator(config.getUserId(), password));

        email.setFrom(config.getUserId(), config.getUserName());
        for (Employee employee : employees) {
            if (employee.getEmail() != null && !employee.getEmail().isBlank()) {
                email.addTo(employee.getEmail(), employee.getName() + " " + employee.getLastName());
            }
        }
        email.setSubject(config.getSubject());
        email.setMsg(config.getMessage());

        email.attach(getAttachment(attachment));

        email.send();

        logger.info("Alle Emails abgeschickt!");
    }

    EmailAttachment getAttachment(Path attachment) {
        EmailAttachment emailAttachment = new EmailAttachment();
        emailAttachment.setPath(attachment.toString());
        emailAttachment.setDisposition(EmailAttachment.ATTACHMENT);
        emailAttachment.setDescription(config.getSubject());
        return emailAttachment;
    }
}
