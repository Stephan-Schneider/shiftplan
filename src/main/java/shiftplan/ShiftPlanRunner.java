package shiftplan;

import freemarker.template.TemplateException;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.jsoup.nodes.Document;
import shiftplan.calendar.Shift;
import shiftplan.calendar.ShiftCalendar;
import shiftplan.calendar.ShiftPlanner;
import shiftplan.data.DocumentParser;
import shiftplan.document.DocGenerator;
import shiftplan.document.TemplateProcessor;
import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;
import shiftplan.users.HomeOfficeRecord;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftPlanRunner {

    private static final Logger logger = LogManager.getLogger(ShiftPlanRunner.class);

    public Map<String, Object> createShiftPlan(DocumentParser docParser) {
        LocalDate startDate = docParser.getStartDate();
        LocalDate endDate = docParser.getEndDate();
        List<LocalDate> holidays = docParser.getHolidays();
        List<EmployeeGroup> groups = docParser.getEmployeeGroupList();
        int homeOfficeDuration = docParser.getHomeOfficeDuration();
        int lateShiftDuration = docParser.getMaxLateShiftDuration();
        int maxPerWeek = docParser.getMaxHomeOfficeDaysPerWeek();
        int maxPerMonth = docParser.getMaxHomeOfficeDaysPerMonth();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, docParser.getYear(), startDate, endDate);

        shiftPlanner.createHomeOfficePlan(groups, homeOfficeDuration);
        groups.forEach(employeeGroup -> {
            List<HomeOfficeRecord> homeOfficeRecords = employeeGroup.calculateHomeOfficeOptionByMonth(
                    docParser.getYear(), maxPerWeek, maxPerMonth
            );
            HomeOfficeRecord.addRecord(homeOfficeRecords);
        });

        Employee[] employees = EmployeeGroup.getEmployeesInShiftOrder();
        shiftPlanner.createLateShiftPlan(employees,lateShiftDuration);

        Map<String, Shift> shiftPlan = shiftPlanner.createShiftPlan(groups);

        ShiftCalendar shiftCalendar = new ShiftCalendar(docParser.getYear());
        Map<Integer, LocalDate[]> calendar = shiftCalendar.createCalendar(startDate, endDate);

        List<Employee> allEmployees = EmployeeGroup.getAllEmployees();

        Map<String, Integer> shiftInfo = new HashMap<>();
        shiftInfo.put("homeOfficeDuration", homeOfficeDuration);
        shiftInfo.put("lateShiftDuration", lateShiftDuration);

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("startDate", startDate);
        dataModel.put("endDate", endDate);
        dataModel.put("shiftInfo", shiftInfo);
        dataModel.put("employees", allEmployees);
        dataModel.put("shiftPlan", shiftPlan);
        dataModel.put("calendar", calendar);
        dataModel.put("homeOfficeRecords", HomeOfficeRecord.getAllRecords());

        return dataModel;
    }

    private void createPDF(String templateDir, Map<String, Object> dataModel) throws IOException, TemplateException {
        TemplateProcessor processor = TemplateProcessor.INSTANCE;
        if (templateDir == null || templateDir.isEmpty()) {
            // Das Template-Verzeichnis wird von der FTL Konfiguration beim Aufruf von <initConfiguration> ohne
            // Parameter gefunden, solange sich die Anwendung nicht in einem JAR-Archiv befindet.
            processor.initConfiguration();
        } else {
            // Wenn die Anwendung in ein JAR-Archiv gepackt ist, muss das Template in einem Ordner außerhalb
            // des Archivs abgelegt werden. Der Pfad zu diesem Ordner muss bei Aufruf von <initConfiguration>
            // angegeben werden.
            processor.initConfiguration(templateDir);
        }
        StringWriter output = processor.processDocumentTemplate(dataModel, "shiftplan.ftl");

        DocGenerator docGenerator = new DocGenerator();
        Document document = docGenerator.getRawHTML(output.toString());
        docGenerator.createPDF(document, Path.of(System.getProperty("user.home"),
                "Schichtplan" + dataModel.get("startDate") + "_bis_" + dataModel.get("endDate") + ".pdf"));
    }

    private DocumentParser getDocumentParser(String xmlPath) throws IOException, JDOMException {
        DocumentParser documentParser;
        if (xmlPath == null || xmlPath.isEmpty()) {
            // Die Instanziierung läuft unter cer Voraussetzung fehlerfrei, dass sich die xml- und xsd-Datei
            // nicht in einem JAR-Archiv befinden
            documentParser = new DocumentParser();
        } else {
            // Wenn die Anwendung in ein JAR-Archiv gepackt ist, kann das im Archiv befindliche Exemplar der
            // shiftplan-xml nicht editiert werden - die Datei muss also in einem außerhalb des Archivs
            // befindlichen Ordner abgelegt werden. Das XML-Schema muss sich im gleichen Ordner befinden.
            String pathToXMLFile = xmlPath + File.separator + "shiftplan.xml";
            String pathToXSDFile = xmlPath + File.separator + "shiftplan.xsd";
            logger.debug("XML-Dateien: XML: {} / XSD: {}", pathToXMLFile, pathToXSDFile);
            documentParser =  new DocumentParser(pathToXMLFile, pathToXSDFile);
        }
        documentParser.parseDocument();
        return documentParser;
    }

    private static Options getCLIArgs() throws ParseException {
        // Definition der Kommandozeilen-Argumente

        Option xmlOption = Option
                .builder("x")
                .longOpt("xmlPath")
                .desc("Enthält shiftplan.xml und shiftplan.xsd. Obligatorisch, wenn Anwendung in JAR gepackt ist")
                .hasArg()
                .argName("Pfad")
                .build();

        Option templateOption = Option.
                builder("t")
                .longOpt("templatePath")
                .desc("Enthält shiftplan.ftl (kann mit XML-Verzeichnis identisch sein). Obligatorisch," +
                        " wenn Anwendung in JAR gepackt ist")
                .hasArg()
                .argName("Pfad")
                .build();

        Option configOption = Option
                .builder("c")
                .longOpt("configPath")
                .desc("Pfad zur Konfigurationsdatei (bei Emailversand aus dem Programm) - optional")
                .hasArg()
                .argName("Pfad")
                .build();

        Option pwdOption = Option
                .builder("p")
                .longOpt("smtpPassword")
                .desc("SMTP-Passwort (bei Emailversand aus dem Programm) - optional. Obligatorisch nur, wenn " +
                        "Emailversand aktiviert")
                .hasArg()
                .argName("SMTP - Passwort")
                .build();

        Option sendMailOption = Option
                .builder("s")
                .longOpt("sendMail")
                .desc("Option nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei" +
                        " mit vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus")
                .hasArg(false)
                .build();

        Option helpOption = new Option("h", "help", false, "Diese Nachricht drucken");

        Options options = new Options();
        options.addOption(helpOption);
        options.addOption(xmlOption);
        options.addOption(templateOption);
        options.addOption(configOption);
        options.addOption(pwdOption);
        options.addOption(sendMailOption);

        return options;
    }

    public static void main(String[] args) {
        // -x, --xmlPath: Enthält shiftplan.xml und shiftplan.xsd. Obligatorisch, wenn Anwendung in JAR gepackt ist.
        // -t, --templatePath: Enthält shiftplan.ftl (kann mit XML-Verzeichnis identisch sein). Obligatorisch,
        //      wenn Anwendung in JAR gepackt ist
        // -c, --configPath: Pfad zur Konfigurationsdatei (bei Emailversand aus dem Programm) - optional
        // -p, --smtpPassword: SMTP-Passwort (bei Emailversand aus dem Programm) - optional. Obligatorisch nur, wenn
        //      Emailversand aktiviert
        // -s, --sendMail: Option nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei
        //      mit vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus
        logger.info("ShiftplanRunner gestartet mit den Argumenten: {}", Arrays.toString(args));

        Options options = null;
        CommandLine cmd = null;
        try {
            options = getCLIArgs();
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException exception) {
            System.out.println("Fehler beim Aufruf der Anwendung: " + exception.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("shiftplan", options, true);
            System.exit(1);
        }

        String xmlPath = null;
        String templatePath = null;
        String configPath = null;
        String password = null;
        boolean sendMail = false;


        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("shiftplan", options, true);
            System.exit(0);
        }
        if (cmd.hasOption("x")) {
            xmlPath = cmd.getOptionValue("x");
        }
        if (cmd.hasOption("t")) {
            templatePath = cmd.getOptionValue("t");
        }

        if (cmd.hasOption("c")) {
            configPath = cmd.getOptionValue("c");
        }

        if (cmd.hasOption("p")) {
            password = cmd.getOptionValue("p");
        }

        if (cmd.hasOption("s")) {
            sendMail = true;
        }

        logger.info("shiftplan mit folgenden Argumenten aufgerufen:");
        logger.info("xmlPath: {}", xmlPath);
        logger.info("templatePath: {}", templatePath);
        logger.info("configPath: {}", configPath);
        logger.info("smtpPassword: {}", password);
        logger.info("sendMail: {}", sendMail);

        ShiftPlanRunner shiftPlanRunner = new ShiftPlanRunner();
        try {
            DocumentParser parser = shiftPlanRunner.getDocumentParser(xmlPath);
            Map<String, Object> dataModel = shiftPlanRunner.createShiftPlan(parser);
            shiftPlanRunner.createPDF(templatePath, dataModel);
        } catch (IOException | JDOMException | TemplateException ex) {
            logger.fatal("shiftplan.xml|shiftplan.xsd können nicht gelesen werden", ex);
        }
    }
}
