package shiftplan.web;

import freemarker.template.TemplateException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.ShiftPlanRunner;
import shiftplan.ShiftPlanRunnerException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class PublishHandler implements HttpHandler {

    private final Logger logger = LogManager.getLogger(PublishHandler.class);
    private final ConfigBundle bundle = ConfigBundle.INSTANCE;

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        PathTemplateMatch pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        Map<String, String> urlParams = pathMatch.getParameters();
        String fileFormat = urlParams.getOrDefault("format", "html");

        Path outFile = Path.of(ShiftplanServer.BASE_PATH, "generated", "Schichtplan." + fileFormat);
        Path shiftplanCopyFile = Path.of(bundle.getShiftPlanCopyXMLFile());

        if (this.isOutdatedShiftplanFile(outFile, shiftplanCopyFile)) {
            try {
                ShiftPlanRunner runner = new ShiftPlanRunner();
                Map<String, Object> dataModel = runner.getShiftplanCopy(bundle.getShiftPlanCopyXMLFile(),
                        bundle.getShiftPlanCopySchemaDir());
                if ("html".equals(fileFormat)) {
                    String shiftplanHtml = runner.processTemplate(
                            bundle.getTemplateDir(), dataModel, "shiftplan_web.ftl");
                    writeHtml(shiftplanHtml, outFile);
                } else if ("pdf".equals(fileFormat)) {
                    runner.createPDF(bundle.getTemplateDir(), dataModel, outFile.getParent().toString());
                }
            } catch(IOException | TemplateException ex){
                logger.error("Fehler bei Verarbeitung des Templates oder Speichern des finalen HTMLs", ex);
                throw new ShiftPlanRunnerException(ex.getMessage());
            }
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
        exchange.getResponseSender().send("{\"link\":\"generated/" + outFile.getFileName() + "\"}");
    }

    private boolean isOutdatedShiftplanFile(Path outFile, Path shiftplanCopyFile) {
        try {
            if (!Files.exists(outFile)) {
                // Die Datei shiftplan.html|.pdf existiert (noch) nicht, muss also generiert werden
                return true;
            }
            BasicFileAttributes outFileAttrs = Files.readAttributes(outFile, BasicFileAttributes.class);
            BasicFileAttributes shiftplanCopyFileAttrs = Files.readAttributes(shiftplanCopyFile, BasicFileAttributes.class);

            // compare < 0: die Html|PDF-Datei (outFile) ist älter als shiftplan_serialized.xml - shiftplan.html|pdf
            // muss neu generiert werden.
            int compare = outFileAttrs.lastModifiedTime().compareTo(shiftplanCopyFileAttrs.lastModifiedTime());
            return compare < 0;
        } catch (IOException e) {
            logger.error("Fehler beim Auslesen von Datei-Attributen", e);
            throw new ShiftPlanRunnerException(e.getMessage());
        }
    }

    private void writeHtml(String shiftplanHtml, Path outFile) throws IOException {
        Path parent = outFile.getParent();
        if (!Files.exists(parent)) {
            // Nur 'generated' Verzeichnis erstellen, alle übergeordneten Verzeichnisse müssen bereits existieren
            Files.createDirectory(parent);
        }
        Files.writeString(
                outFile,
                shiftplanHtml,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }
}
