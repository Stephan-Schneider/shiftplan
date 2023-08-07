package shiftplan;

import freemarker.template.TemplateException;
import org.apache.commons.cli.*;
import org.apache.commons.mail.EmailException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.jsoup.nodes.Document;
import shiftplan.calendar.*;
import shiftplan.data.InvalidShiftPlanException;
import shiftplan.data.ShiftPlanDescriptor;
import shiftplan.data.ShiftPlanSerializer;
import shiftplan.document.DocGenerator;
import shiftplan.document.TemplateProcessor;
import shiftplan.publish.EmailDispatch;
import shiftplan.users.Employee;
import shiftplan.users.HomeOfficeRecord;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftPlanRunner {

    private static final Logger logger = LogManager.getLogger(ShiftPlanRunner.class);

    public Map<String, Object> createShiftPlan(String xmlPath, SwapParams swapParams) {
        assert swapParams.getMode() == OP_MODE.CREATE;

        ShiftPlanDescriptor descriptor = getShiftPlanDescriptor(xmlPath);

        int year = descriptor.getYear();
        LocalDate startDate = descriptor.getStartDate();
        LocalDate endDate = descriptor.getEndDate();
        List<LocalDate> holidays = descriptor.getHolidays();
        Employee[] employees = descriptor.getEmployees();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, year, startDate, endDate);

        Map<String, Shift> shiftPlan = shiftPlanner.createLateShiftPlan(employees);

        ShiftCalendar shiftCalendar = new ShiftCalendar(year);
        Map<Integer, LocalDate[]> calendar = shiftCalendar.createCalendar(startDate, endDate);

        shiftPlanner.createHomeOfficePlan(employees, shiftPlan, calendar);

        HomeOfficeRecord.createHomeOfficeReport(employees, startDate, endDate);
        List<HomeOfficeRecord> records = HomeOfficeRecord.getAllRecords();

        ShiftPlanSerializer serializer = new ShiftPlanSerializer(swapParams.getShiftplanCopyXMLFile());
        org.jdom2.Document doc = serializer.serializeShiftPlan(
                year,
                startDate,
                endDate,
                shiftPlan,
                calendar,
                employees
        );

        try {
            serializer.writeXML(doc);
        } catch (IOException ex) {
            logger.error("Ausnahme beim Schreiben der shiftplan - XML-Datei", ex);
            throw new ShiftPlanRunnerException(ex.getMessage());
        }

        Map<String, Object> dataModel = createDataModel(startDate, endDate, employees, shiftPlan, calendar);
        dataModel.put("homeOfficeRecords", records);

        return dataModel;
    }

    public Map<String, Object> modifyShiftPlan(SwapParams swapParams) {
        OP_MODE mode = swapParams.getMode();
        Path XMLFile = swapParams.getShiftplanCopyXMLFile();
        Path XSDDir = swapParams.getShiftPlanCopySchemaDir();

        try {
            String[] employeeSet1 = swapParams.getEmployeeSet1();
            String[] employeeSet2 = swapParams.getEmployeeSet2();
            boolean swapHo = swapParams.isSwapHo();

            ShiftPlanSerializer serializer = new ShiftPlanSerializer(XMLFile, XSDDir);
            ShiftPlanCopy copy = serializer.deserializeShiftplan();

            ShiftSwap swapper = new ShiftSwap(copy, mode, swapHo);
            ShiftSwap.SwapResult swapResult;
            if (mode == OP_MODE.REPLACE) {
                swapResult = swapper.swap(
                        employeeSet1[0],
                        Integer.parseInt(employeeSet1[1]),
                        employeeSet2[0]
                );
            } else {
                swapResult = swapper.swap(
                        employeeSet1[0],
                        Integer.parseInt(employeeSet1[1]),
                        employeeSet2[0],
                        Integer.parseInt(employeeSet2[1])
                );
            }
            ShiftSwapDataModelConverter converter = new ShiftSwapDataModelConverter(copy, swapper);

            org.jdom2.Document document = serializer.serializeShiftPlan(converter);
            serializer.writeXML(document);

            Map<String, Object> dataModel = createDataModel(converter);
            dataModel.put("swapResult", swapResult);
            return dataModel;
        } catch (NullPointerException | IllegalArgumentException | IOException | JDOMException ex) {
            throw new ShiftPlanSwapException(ex.getMessage());
        } catch (IndexOutOfBoundsException ex) {
            String error = "Die employeeA-Daten müssen immer die ID sowie den KW-Index enthalten. Die employeeB-Daten " +
                    "müssen im Operation-Modus SWAP die ID und den KW-Index, im REPLACE-Modus die ID enthalten";
            throw new ShiftPlanSwapException(error);
        }
    }

    Map<String, Object> createDataModel(ShiftSwapDataModelConverter converter) {
        return createDataModel(
                converter.getStartDate(),
                converter.getEndDate(),
                converter.getEmployees(),
                converter.getShiftPlan(),
                converter.getCalendar());
    }

    Map<String, Object> createDataModel(
            LocalDate from,
            LocalDate to,
            Employee[] employees,
            Map<String, Shift> shiftPlan,
            Map<Integer, LocalDate[]> calendar
    ) {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("startDate", from);
        dataModel.put("endDate", to);
        dataModel.put("shiftInfo", ShiftSwapDataModelConverter.getShiftInfo());
        dataModel.put("employees", employees);
        dataModel.put("shiftPlan", shiftPlan);
        dataModel.put("calendar", calendar);
        return dataModel;

    }

    Path createPDF(String templateDir, Map<String, Object> dataModel, String outDir)
            throws IOException, TemplateException {
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

        // Pfad für den zu erstellenden Schichtplan
        Path pathToPDF;
        String fileName = "Schichtplan_" + dataModel.get("startDate") + "_bis_" + dataModel.get("endDate");
        if (outDir == null || outDir.isEmpty()) {
            pathToPDF = Files.createTempFile(fileName, ".pdf");
            pathToPDF.toFile().deleteOnExit();
        } else {
            pathToPDF = Path.of(outDir, fileName + ".pdf");
        }

        DocGenerator docGenerator = new DocGenerator();
        Document document = docGenerator.getRawHTML(output.toString());
        docGenerator.createPDF(document, pathToPDF);

        return pathToPDF;
    }

    private ShiftPlanDescriptor getShiftPlanDescriptor(String xmlPath) {
        ShiftPlanDescriptor descriptor;
        if (xmlPath == null || xmlPath.isEmpty()) {
            // Die Instanziierung läuft unter cer Voraussetzung fehlerfrei, dass sich die xml- und xsd-Datei
            // nicht in einem JAR-Archiv befinden
            descriptor = new ShiftPlanDescriptor();
        } else {
            // Wenn die Anwendung in ein JAR-Archiv gepackt ist, kann das im Archiv befindliche Exemplar der
            // shiftplan-xml nicht editiert werden - die Datei muss also in einem außerhalb des Archivs
            // befindlichen Ordner abgelegt werden. Das XML-Schema muss sich im gleichen Ordner befinden.
            descriptor =  new ShiftPlanDescriptor(Path.of(xmlPath));
        }
        try {
            descriptor.parseDocument();
        } catch (IOException | JDOMException ex) {
            logger.error("Kann keinen ShiftPlan-Descriptor generieren!");
            throw new ShiftPlanRunnerException(ex.getMessage());
        }
        return descriptor;
    }

    public SwapParams getOperationalParams() {
        return SwapParams.readSwapParams();
    }

    public SwapParams getOperationalParams(String path) {
        return SwapParams.readSwapParams(path);
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

        Option outDirOption = Option
                .builder("o")
                .longOpt("outDir")
                .desc("Pfad zum Speicherort (Verzeichnis) der Shiftplan-Datei. Falls nicht angegeben wird eine " +
                        "temporäre Datei im Standardverzeichnis für Temp-Dateien des jeweiligen Betriebssystems " +
                        "angelegt")
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
        options.addOption(outDirOption);
        options.addOption(configOption);
        options.addOption(pwdOption);
        options.addOption(sendMailOption);


        return options;
    }

    public static void main(String[] args) {
        // -x, --xmlPath: Enthält shiftplan.xml und shiftplan.xsd. Obligatorisch, wenn Anwendung in JAR gepackt ist.
        // -t, --templatePath: Enthält shiftplan.ftl (kann mit XML-Verzeichnis identisch sein). Obligatorisch,
        //          wenn Anwendung in JAR gepackt ist
        // -o, --outDir: Pfad zum Speicherort (Verzeichnis) der Shiftplan-Datei. Falls nicht angegeben, wird eine
        //          temporäre Datei im Standardverzeichnis für Temp-Dateien des jeweiligen Betriebssystems angelegt
        // -c, --configPath: Pfad zur Konfigurationsdatei (bei Emailversand aus dem Programm) - optional
        // -p, --smtpPassword: SMTP-Passwort (bei Emailversand aus dem Programm) - optional. Obligatorisch nur, wenn
        //          Emailversand aktiviert
        // -s, --sendMail: Option nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei
        //          mit vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus

        // Nicht auskommentieren - keine Ausgabe des Passworts in Klartext !!
        // logger.trace("ShiftplanRunner gestartet mit den Argumenten: {}", Arrays.toString(args));

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
        String outDir = null;
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

        if (cmd.hasOption("o")) {
            outDir = cmd.getOptionValue("o");
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
        logger.info("outDir:  {}", outDir);
        logger.info("configPath: {}", configPath);
        logger.info("sendMail: {}", sendMail);



        ShiftPlanRunner shiftPlanRunner = new ShiftPlanRunner();
        try {
            SwapParams swapParams = shiftPlanRunner.getOperationalParams();
            Map<String, Object> dataModel;
            if (swapParams.getMode() == OP_MODE.CREATE) {
                dataModel = shiftPlanRunner.createShiftPlan(xmlPath, swapParams);
            } else {
                dataModel = shiftPlanRunner.modifyShiftPlan(swapParams);
            }
            Path attachment = shiftPlanRunner.createPDF(templatePath, dataModel, outDir);
            logger.info("Neuer Schichtplan in '{}' gespeichert", attachment.toString());

            if (sendMail) {
                if (password != null && !password.isBlank()) {
                    Employee[] employees = (Employee[]) dataModel.get("employees");
                    EmailDispatch emailDispatch = new EmailDispatch(configPath);
                    emailDispatch.sendMail(List.of(employees), attachment, password);
                }
            }
        } catch (IOException ex) {
            logger.fatal("XML/XSD-Datei kamm nicht gelesen oder geschrieben werden", ex);
        } catch (TemplateException ex) {
            logger.fatal("Das FTL-Template kann nicht verarbeitet werden", ex);
        } catch (EmailException ex) {
            logger.error("Der Emailversand des Schichtplans ist gescheitert", ex);
        } catch (ShiftPlanRunnerException | InvalidShiftPlanException | ShiftPlanSwapException ex) {
            logger.fatal(ex.getMessage());
        }
    }
}
