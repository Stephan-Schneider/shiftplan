package shiftplan.web;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.ShiftPlanRunner;
import shiftplan.ShiftPlanRunnerException;
import shiftplan.data.json.ShiftplanDescriptorJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;


public class CreateHandler implements HttpHandler {

    private static final Logger logger = LogManager.getLogger(CreateHandler.class);
    private final ConfigBundle config = ConfigBundle.INSTANCE;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toString();

        if (method.equalsIgnoreCase("get")) {
            String shiftplanDescription = this.readDescription();
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
            exchange.getResponseSender().send(shiftplanDescription);
        } else if (method.equalsIgnoreCase("post")) {
            PathTemplateMatch pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
            Map<String, String> urlParams = pathMatch.getParameters();
            // Existierende shiftplan.json löschen ? (In bestimmten Fällen notwendig, vor allem wenn
            // ein neuer Plan im gleichen Zeitraum eines bestehenden Plans erstellt wird)
            boolean clear = Boolean.parseBoolean(urlParams.getOrDefault("clear", "false"));
            if (clear) {
                deleteShiftplanJson();
            }
            exchange.getRequestReceiver().receiveFullBytes((e, m) -> {
                String data = new String(m, StandardCharsets.UTF_8);
                logger.info("Json: {}", data);
                ShiftplanDescriptorJson descriptor = ShiftplanDescriptorJson.readObject(data);
                new ShiftPlanRunner().createShiftplan(descriptor, config.getShiftPlanCopyXMLFile(),
                        config.getShiftPlanCopySchemaDir());
                try {
                    this.writeDescription(data);
                } catch (IOException ex) {
                    logger.error("Request body kann nicht gelesen werden", ex);
                    throw new ShiftPlanRunnerException(ex.getMessage());
                }
            });
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            exchange.getResponseSender().send("Schichtplan-Datei gespeichert");
        }
    }

    String readDescription() throws IOException {
        String pathString = config.getJsonFile() == null ? "" : config.getJsonFile();
        if (pathString.isEmpty()) return "";
        Path jsonPath = Path.of(pathString);
        String shiftplanDescription = "{}";
        if (Files.exists(jsonPath)) {
            shiftplanDescription = Files.readString(jsonPath, StandardCharsets.UTF_8);
            logger.info("Schichtplan-Beschreibung aus Datei '{}' gelesen", jsonPath);
            return shiftplanDescription;
        }
        logger.info("Es existiert noch keine Schichtplan-Beschreibungsdatei");
        return shiftplanDescription;
    }

    void writeDescription(String description) throws IOException {
        if (description == null || description.isEmpty() || "{}".equals(description)) return;

        String pathString = config.getJsonFile() == null ? "" : config.getJsonFile();
        if (pathString.isEmpty()) return;
        pathString = pathString.substring(0, pathString.indexOf(".")) + "_test.json";

        Path jsonPath = Path.of(pathString);
        Path parentDir = jsonPath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        Files.writeString(
                jsonPath,
                description,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private void deleteShiftplanJson() {
        String pathString = config.getJsonFile() == null ? "" : config.getJsonFile();
        if (pathString.isEmpty()) return;

        try {
            Files.deleteIfExists(Path.of(pathString));
        } catch (IOException e) {
            throw new ShiftPlanRunnerException(e.getMessage());
        }
    }
}
