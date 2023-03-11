package shiftplan.document;

import freemarker.template.*;
import no.api.freemarker.java8.Java8ObjectWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public enum TemplateProcessor {

    INSTANCE;
    private Configuration cfg;
    private boolean configured;

    private static final Logger logger = LogManager.getLogger(TemplateProcessor.class);

    public boolean initConfiguration() throws IOException {
        return initConfiguration(null);
    }

    public boolean initConfiguration(String templateDir) throws IOException {
        if (cfg != null && configured) return true;

        File templateDirFile;
        if (templateDir == null) {
            templateDirFile = getTemplateDir();
        } else {
            templateDirFile = getTemplateDir(templateDir);
        }

        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setDirectoryForTemplateLoading(templateDirFile);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setObjectWrapper(new Java8ObjectWrapper(Configuration.VERSION_2_3_31)); // java.time.LocalDate - Formatierung !!
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);

        configured = true;
        return true;
    }

    private File getTemplateDir() {
        String templateDir = Objects.requireNonNull(getClass().getResource("templates")).getFile();
        logger.info("Template - Verzeichnis: {} wird gesetzt", templateDir);
        return new File(templateDir);
    }

    private File getTemplateDir(String templateDir) {
        Path p;
        if (templateDir == null || templateDir.isEmpty()) {
            throw new IllegalArgumentException("Kein Template-Verzeichnis angegeben!");
        }
        p = Path.of(templateDir);
        if (!Files.isDirectory(p)) {
            throw new IllegalArgumentException("Kein gültiger Pfad zum Template-Verzeichnis: " + templateDir);
        }
        logger.info("Template - Verzeichnis {} wird gesetzt", p.toString());
        return p.toFile();
    }

    public StringWriter processDocumentTemplate(Map<String, Object> dataModel, String templateFile)
            throws IOException, TemplateException, IllegalStateException {
        /*
        dataModel.put("startDate", start) // LocalDate
        dataModel.put("endDate", end)  // LocalDate
        dataModel.put("shiftInfo", Map<String, Integer>);
        dataModel.put("employees", employees) // Array
        dataModel.put("shiftPlan", shiftPlan) // HashMap<String, Shift>
        dataModel.put("calendar", calendar) // Map<Integer, LocalDate[]>
        dataModel.put("homeOfficeRecords", List<HomeOfficeRecords>);
         */
        assert dataModel != null && !dataModel.isEmpty();
        assert templateFile != null && templateFile.endsWith(".ftl");
        logger.info("Das Template {} wird verarbeitet ...", templateFile);

        if (!configured) throw new IllegalStateException("Keine FreeMarker Template-Konfiguration!");

        try (StringWriter output = new StringWriter()) {
            Template template = cfg.getTemplate(templateFile);
            template.process(dataModel, output);
            return output;
        } catch (IOException | TemplateException exception) {
            logger.error("Fehler beim Laden / Parsen des Templates:", exception);
            throw exception;
        }
    }
}
