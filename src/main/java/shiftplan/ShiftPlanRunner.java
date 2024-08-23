package shiftplan;

import freemarker.template.TemplateException;
import org.apache.commons.cli.*;
import org.apache.commons.mail.EmailException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.jsoup.nodes.Document;
import shiftplan.calendar.*;
import shiftplan.data.IShiftplanDescriptor;
import shiftplan.data.InvalidShiftPlanException;
import shiftplan.data.json.ShiftplanDescriptorJson;
import shiftplan.data.xml.ShiftPlanDescriptor;
import shiftplan.data.ShiftPlanSerializer;
import shiftplan.document.DocGenerator;
import shiftplan.document.TemplateProcessor;
import shiftplan.publish.EmailDispatch;
import shiftplan.users.Employee;
import shiftplan.users.HomeOfficeRecord;
import shiftplan.users.StaffList;
import shiftplan.web.ConfigBundle;
import shiftplan.web.ShiftplanServer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ShiftPlanRunner {

    private static final Logger logger = LogManager.getLogger(ShiftPlanRunner.class);

    public void createShiftPlan(String xmlPath, String shiftPlanCopyXMLFile, SwapParams swapParams) {
        if (swapParams.getMode() != OP_MODE.CREATE) {
            throw new ShiftPlanRunnerException("Ungültiger Operation-Mode: " +  swapParams.getMode().toString());
        }

        IShiftplanDescriptor descriptor = getShiftPlanDescriptor(xmlPath);
        this.createShiftplan(descriptor, shiftPlanCopyXMLFile, xmlPath);
    }

    public void createShiftplan(IShiftplanDescriptor descriptor, String shiftPlanCopyXMLFile, String xmlPath) {
        if (descriptor == null) {
            throw new ShiftPlanRunnerException("Keine Schichtplan-Beschreibungsdaten vorhanden!");
        }

        int year = descriptor.getYear();
        BoundaryHandler boundaryHandler = new BoundaryHandler(
                descriptor.getStartDate(),
                descriptor.getEndDate(),
                shiftPlanCopyXMLFile,
                xmlPath
        );
        boundaryHandler.setBoundaryStrict(descriptor.isBoundaryStrict());
        LocalDate startDate = boundaryHandler.getStartDate();
        LocalDate endDate = boundaryHandler.getEndDate();
        List<LocalDate> holidays = descriptor.getHolidays();
        Employee[] employees = descriptor.getEmployees();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, year, startDate, endDate);

        Map<String, Shift> shiftPlan = shiftPlanner.createLateShiftPlan(employees);

        ShiftCalendar shiftCalendar = new ShiftCalendar(year);
        Map<Integer, LocalDate[]> calendar = shiftCalendar.createCalendar(startDate, endDate);

        shiftPlanner.createHomeOfficePlan(employees, shiftPlan, calendar);

        ShiftPlanSerializer serializer = new ShiftPlanSerializer(shiftPlanCopyXMLFile);
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
    }

    public void modifyShiftPlan(String shiftPlanCopyXMLFile, String shiftPlanCopySchemaDir,
                                               SwapParams swapParams) {
        OP_MODE mode = swapParams.getMode();

        try {
            String[] employeeSet1 = swapParams.getEmployeeSet1();
            String[] employeeSet2 = swapParams.getEmployeeSet2();
            boolean swapHo = swapParams.isSwapHo();

            ShiftPlanSerializer serializer = new ShiftPlanSerializer(shiftPlanCopyXMLFile, shiftPlanCopySchemaDir);
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
            // Das Ergebnis des HO-Tauschs wird nicht (mehr) im generierten Schichtplan angezeigt, sondern nur als
            // Information in die Log-datei geschrieben
            logger.info(swapResult);

            ShiftSwapDataModelConverter converter = new ShiftSwapDataModelConverter(copy);

            org.jdom2.Document document = serializer.serializeShiftPlan(converter);
            serializer.writeXML(document);
        } catch (NullPointerException | IllegalArgumentException | IOException | JDOMException ex) {
            throw new ShiftPlanSwapException(ex.getMessage());
        } catch (IndexOutOfBoundsException ex) {
            String error = "Die employeeA-Daten müssen immer die ID sowie den KW-Index enthalten. Die employeeB-Daten " +
                    "müssen im Operation-Modus SWAP die ID und den KW-Index, im REPLACE-Modus die ID enthalten";
            throw new ShiftPlanSwapException(error);
        }
    }

    public Map<String, Object> getShiftplanCopy(String shiftPlanCopyXMLFile, String shiftPlanCopySchemaDir) {
        ShiftPlanSerializer serializer = new ShiftPlanSerializer(shiftPlanCopyXMLFile, shiftPlanCopySchemaDir);
        try {
            ShiftPlanCopy copy = serializer.deserializeShiftplan();
            ShiftSwapDataModelConverter converter = new ShiftSwapDataModelConverter(copy);
            Map<String, Object> dataModel = createDataModel(converter);
            return dataModel;
        } catch (IOException | JDOMException e) {
            throw new ShiftPlanRunnerException(e.getMessage());
        }
    }

    public void createStaffList(String shiftPlanCopyXMLFile, String shiftPlanCopySchemaDir, String staffListDir) {
        try {
            ShiftPlanSerializer serializer = new ShiftPlanSerializer(shiftPlanCopyXMLFile, shiftPlanCopySchemaDir);
            ShiftPlanCopy copy = serializer.deserializeShiftplan();

            StaffList staffList = new StaffList(copy, staffListDir);
            Map<String, StaffList.StaffData> employeeList = staffList.createStaffList();
            String printed = staffList.printStaffList(employeeList);
            staffList.writeToFile(printed);
        } catch (NullPointerException | IllegalArgumentException | IOException | JDOMException ex) {
            throw new ShiftPlanRunnerException(ex.getMessage());
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

        HomeOfficeRecord.createHomeOfficeReport(employees, from, to);
        List<HomeOfficeRecord> records = HomeOfficeRecord.getAllRecords();
        dataModel.put("homeOfficeRecords", records);

        return dataModel;
    }

    public Path createPDF(String templateDir, Map<String, Object> dataModel, String outDir)
            throws IOException, TemplateException {
        String output = processTemplate(templateDir, dataModel);

        // Pfad für den zu erstellenden Schichtplan
        Path pathToPDF;
        //String fileName = "Schichtplan_" + dataModel.get("startDate") + "_bis_" + dataModel.get("endDate");
        String fileName = "Schichtplan"; // Vereinfachter Name im Web-Modus
        if (outDir == null || outDir.isEmpty()) {
            pathToPDF = Files.createTempFile(fileName, ".pdf");
            pathToPDF.toFile().deleteOnExit();
        } else {
            pathToPDF = Path.of(outDir, fileName + ".pdf");
        }

        DocGenerator docGenerator = new DocGenerator();
        Document document = docGenerator.getRawHTML(output);
        docGenerator.createPDF(document, pathToPDF);

        return pathToPDF;
    }

    public String processTemplate(String templateDir, Map<String, Object> dataModel, String... templateFileNames)
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
        // Optional kann ein alternatives Template angegeben werden, z.B. 'shiftplan_web.ftl' für die Erstellung eines
        // Schichtplans im HTML-Format
        String fileName = templateFileNames.length > 0 ? templateFileNames[0] : "shiftplan.ftl";
        StringWriter output = processor.processDocumentTemplate(dataModel, fileName);
        return output.toString();
    }

    private IShiftplanDescriptor getShiftPlanDescriptor(String xmlPath) {
        ConfigBundle bundle = ConfigBundle.INSTANCE;
        if (bundle.getJsonFile() != null && !bundle.getJsonFile().isEmpty()) {
            // Die Schichtplan - Datei im JSON-Format (shiftplan.json) hat Vorrang vor der äquivalenten XML-Datei
            // (shiftplan.xml).
            // Die JSON-Datei ist in erster Linie für die Ausführung der Anwendung im Web-Modus gedacht, kann aber auch
            // verwendet werden, wenn die Anwendung lokal ausgeführt wird.
            return ShiftplanDescriptorJson.readObject();
        }

        ShiftPlanDescriptor descriptor = getXMLDescriptor(xmlPath);
        try {
            descriptor.parseDocument();
        } catch (IOException | JDOMException ex) {
            logger.error("Kann keinen ShiftPlan-Descriptor generieren!");
            throw new ShiftPlanRunnerException(ex.getMessage());
        }
        return descriptor;
    }

    private static ShiftPlanDescriptor getXMLDescriptor(String xmlPath) {
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
        return descriptor;
    }


    public SwapParams getOperationalParams() {
        // swap_params.json als Resource im Klassenpfad (in erster Linie für Entwicklungs-/Testzwecke)
        return SwapParams.readSwapParams();
    }

    public SwapParams getOperationalParams(String paramString) {
        // Swap - Parameter als String, der als CLI-Parameter an das Programm übergeben wird
        return SwapParams.readFromString(paramString);
    }

    public SwapParams getOperationalParams(Path path) {
        // swap_params.json mit Angabe eines Dateipfads
        return SwapParams.readSwapParams(path);
    }

    private static Path createGeneratedDataDir(String generatedDataDir) throws IOException, IllegalArgumentException{
        if (generatedDataDir == null || generatedDataDir.isBlank()) {
            throw new IllegalArgumentException("Kein gültiger Wert für generated data - Verzeichnis");
        }

        Path path = Path.of(generatedDataDir);
        if (Files.isDirectory(path) && Files.isReadable(path) && Files.isWritable(path)) {
            return path;
        }
        Path newPath = Files.createDirectories(path);
        logger.info("Erstelltes Generated - Data Verzeichnis: {}", newPath);
        return newPath;
    }

    private static Options getCLIArgs() throws ParseException {
        // Definition der Kommandozeilen-Argumente

        Option xmlOption = Option
                .builder("x")
                .longOpt("xmlPath")
                .desc("Pfad zum XML-Ordner. Enthält shiftplan.xml, shiftplan.xsd und shiftplan_serialized.xsd." +
                        " Obligatorisch, wenn der Ordner außerhalb des Installationsverzeichnis liegt")
                .hasArg()
                .argName("Pfad")
                .build();

        Option templateOption = Option.
                builder("t")
                .longOpt("templatePath")
                .desc("Enthält shiftplan.ftl. Obligatorisch, wenn Anwendung in JAR gepackt ist")
                .hasArg()
                .argName("Pfad")
                .build();

        Option outDirOption = Option
                .builder("g")
                .longOpt("generatedData")
                .desc("Pfad zu den von der Anwendung generierten Dateien (shiftplan.json, shiftplan_serialized.xml," +
                        " Schichtplan.html|.pdf.")
                .hasArg()
                .argName("Pfad")
                .build();

        Option configOption = Option
                .builder("e")
                .longOpt("mailConfigPath")
                .desc("Pfad zur Email-Konfigurationsdatei (bei Emailversand aus dem Programm) - optional")
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

        Option swapParamsOption = Option
                .builder("m") // modify
                .longOpt("swapData")
                .desc("String der sämtliche Parameter für die Modifikation eines Schichtplans enthält")
                .hasArg()
                .argName("Swap-Parameter")
                .build();

        Option swapParamJSONOption = Option
                .builder("j")
                .longOpt("jsonSwapData")
                .desc("Anstatt als CLI-Parameter (-m, --swapData) übergeben zu werden, können die Swap-Parameter auch" +
                        "vor Aufruf des Programms in einer Json-Datei (swap_params.json) hinterlegt werden. In diesem" +
                        "Fall ist jedoch der Pfad zur JSON-Datei anzugeben. Der Programmaufruf kann entweder mit dem Pfad" +
                        "zu dieser Datei oder mit der direkten Übergabe der Parameter auf der Kommandozeile erfolgen")
                .hasArg()
                .argName("Swap-Parameter (JSON)")
                .build();

        Option webResourcesOption = Option
                .builder("w")
                .longOpt("webResourcesBasePath")
                .desc("Basispfad für das öffentliche Verzeichnis, in dem sich die Webressourcen befinden")
                .hasArg()
                .argName("webResourcesPath")
                .build();

        Option useJsonShiftplanOption = Option
                .builder("u")
                .longOpt("useJsonShiftlanConfig")
                .desc("Die Datei shiftplan.json wird anstelle von shiftplan.xml als Konfigurationsdatei verwendet")
                .hasArg()
                .argName("userJsonShiftplan")
                .build();

        Option createStafflistOption = Option
                .builder("l")
                .longOpt("createStafflist")
                .desc("Erstelle eine Mitarbeiter-Liste mit den jeweiligen Kalenderwochen bei lokaler Ausführung")
                .hasArg(false)
                .argName("createStafflist")
                .build();

        Option serverOption = Option
                .builder("S") // start web server
                .desc("Startet einen Webserver zur Ausführung der Anwendung als Web-Service. Die Anwendung" +
                        " benutzt im Server-Modus die shiftplan.json - Datei zum Lesen und Schreiben der" +
                        " Schichtplan-Konfiguration")
                .hasArg(false)
                .argName("startServer")
                .build();

        Option hostOption = Option
                .builder("H")
                .desc("Host-Adresse, an die der Server-Socket gebunden wird (localhost|127.0.0.1 oder 0.0.0.0)")
                .hasArg()
                .argName("serverHost")
                .build();

        Option portOption = Option
                .builder("P")
                .desc("Startet den Webserver an Port <port>")
                .hasArg()
                .argName("serverPort")
                .type(Integer.class)
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
        options.addOption(swapParamsOption);
        options.addOption(swapParamJSONOption);
        options.addOption(webResourcesOption);
        options.addOption(useJsonShiftplanOption);
        options.addOption(createStafflistOption);
        options.addOption(serverOption);
        options.addOption(hostOption);
        options.addOption(portOption);

        return options;
    }

    public static void main(String[] args) {
        // -x, --xmlPath: Enthält shiftplan.xml, shiftplan.xsd und shiftplan_serialized.xsd. Obligatorisch, wenn
        //          Anwendung in JAR gepackt ist.
        // -t, --templatePath: Enthält shiftplan.ftl. Obligatorisch, wenn Anwendung in JAR gepackt ist
        // -g, --generatedData: Pfad zu den von der Anwendung generierten Dateien (shiftplan.json,
        //          shiftplan_serialized.xml, Schichtplan.html|.pdf.
        // -e, --configPath: Pfad zur SMTP-Konfigurationsdatei (bei Emailversand aus dem Programm) - optional
        // -p, --smtpPassword: SMTP-Passwort (bei Emailversand aus dem Programm) - optional. Obligatorisch nur, wenn
        //          Emailversand aktiviert
        // -s, --sendMail: Option nur angeben, wenn Emailversand aktiviert werden soll - setzt eine Konfigurationsdatei
        //          mit vollständigen SMTP-Parametern und Angabe eines gültigen Passworts voraus
        // -m, --swapData: Swap-Parameter-String der sämtliche Parameter für die Modifikation eines Schichtplans enthält.
        //          Die Swap-Parameter (zur Modifikation des Schichtplans) können entweder mithilfe dieses CLI-Parameters
        //          oder mit einer JSON-Datei (swap_params.json) an das Programm übergeben werden
        // -j, --jsonSwapData: Anstatt als CLI-Parameter (-m, --swapData) übergeben zu werden, können die Swap-Parameter
        //          auch vor Aufruf des Programms in einer Json-Datei (swap_params.json) hinterlegt werden. In diesem
        //          Fall ist jedoch der Pfad zur JSON-Datei anzugeben. Der Programmaufruf kann entweder mit dem Pfad zu
        //          dieser Datei oder mit der direkten Übergabe der Parameter auf der Kommandozeile erfolgen (bei
        //          entferntem Aufruf des Programms via SSH müssen die Parameter als Kommandozeilen-Parameter übergeben
        //          werden
        // -w, --webResourcesBasePath: Basispfad für das öffentliche Verzeichnis, in dem sich die Webressourcen befinden
        // -u, --useJsonShiftlanConfig: Benutze die Datei shiftplan.json anstatt shiftplan.xml als
        //          Schichtplan-Konfigurationsdatei
        // -, --createStafflist: Erstelle eine Mitarbeiter-Liste mit den jeweiligen Kalenderwochen für die lokale
        //          Ausführung
        // -S:      Startet einen Webserver für den Remote-Zugriff zum Ändern eines Schichtplans. Die Anwendung benutzt
        //          im Server-Modus die shiftplan.json - Datei zum Lesen und Schreiben der Schichtplan-Konfiguration
        // -H       Host-Adresse, an die der Server-Socket gebunden wird (localhost, 127.0.0.1 oder 0.0.0.0)
        // -P:      Startet den Webserver an Port <port>

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
        String generatedData = null;
        String mailConfigPath = null;
        String password = null;
        boolean sendMail = false;
        String swapParamsString = null;
        Path swapParamsFile = null;
        String webResourcesBasePath = null;
        boolean useJsonShiftplanConfig = false;
        boolean createStafflist = false;

        boolean startServer = false;
        String host = "localhost";
        int port = 8080;

        String shiftPlanCopyXMLFile;
        String shiftplanJsonFile;
        String stafflistDir;

        Objects.requireNonNull(cmd, "Keine Kommandozeile generiert!");

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

        if (cmd.hasOption("g")) {
            generatedData = cmd.getOptionValue("g");
        }

        if (cmd.hasOption("e")) {
            mailConfigPath = cmd.getOptionValue("e");
        }

        if (cmd.hasOption("p")) {
            password = cmd.getOptionValue("p");
        }

        if (cmd.hasOption("s")) {
            sendMail = true;
        }

        if (cmd.hasOption("m")) {
            swapParamsString = cmd.getOptionValue("m");
        }

        if (cmd.hasOption("j")) {
            swapParamsFile = Path.of(cmd.getOptionValue("j"));
            Objects.requireNonNull(swapParamsFile, "Ungültiger Wert für Swap-Datei");
            if (swapParamsFile.toString().isEmpty()) {
                System.out.println("Ungültiger Wert für Swap-Datei. Das Programm wird beendet");
                System.exit(1);
            }
        }

        if (cmd.hasOption("w")) {
            webResourcesBasePath = cmd.getOptionValue("w");
        }

        if (cmd.hasOption("u")) {
            useJsonShiftplanConfig = Boolean.parseBoolean(cmd.getOptionValue("u"));
        }

        if (cmd.hasOption("l")) {
            createStafflist = true;
        }

        if (cmd.hasOption("S")) {
            startServer = true;
        }

        if (cmd.hasOption("H")) {
            host = cmd.getOptionValue("H");
        }

        if (cmd.hasOption("P")) {
            try {
                port = cmd.getParsedOptionValue("P");
            } catch (ParseException e) {
                throw new ShiftPlanRunnerException(e.getMessage());
            }
        }

        logger.info("shiftplan mit folgenden Argumenten aufgerufen:");
        logger.info("xmlPath: {}", xmlPath);
        logger.info("templatePath: {}", templatePath);
        logger.info("generatedData:  {}", generatedData);
        logger.info("mailConfigPath: {}", mailConfigPath);
        logger.info("sendMail: {}", sendMail);
        logger.info("Per CLI übergebene Swap-Parameter: {}", swapParamsString);
        logger.info("Per swap_parameter.json übergebene Swap-Parameter: {}", swapParamsFile);
        logger.info("webResourcesBasePath: {}", webResourcesBasePath);
        logger.info("use shiftplan.json: {}", useJsonShiftplanConfig);
        logger.info("create stafflist: {}", createStafflist);
        logger.info("Anwendung läuft im Web-Server - Modus: {}", startServer);
        if (startServer) {
            logger.info("Web-Server an {} wird gestartet an Port: {}", host, port);
        }

        try {
            Path genDirPath = createGeneratedDataDir(generatedData);
            shiftPlanCopyXMLFile = genDirPath.resolve("shiftplan_serialized.xml").toString();
            shiftplanJsonFile = genDirPath.resolve("shiftplan.json").toString();
            stafflistDir = generatedData;
        } catch (IOException e) {
            throw new ShiftPlanRunnerException(e.getMessage());
        }

        if (startServer) {
            new ConfigBundle.ConfigBuilder(
                    shiftplanJsonFile, shiftPlanCopyXMLFile, xmlPath)
                    .templateDir(templatePath)
                    .generatedDataDir(generatedData)
                    .webResourcesBasePath(webResourcesBasePath)
                    .smtpConfigPath(mailConfigPath)
                    .build();
            ShiftplanServer.createServer(host, port);
            return;
        }

        if (useJsonShiftplanConfig) {
            // Es wird ein ConfigBundle mit einem einzigen Parameter (shiftplanJsonConfigFile) erstellt.
            // Dieses ConfigBundle kann nur bei lokaler Ausführung der Anwendung eingesetzt werden, um shiftplan.json
            // anstatt shiftplan.xml als Schichtplan-Konfigurationsdatei zu verwenden.
            new ConfigBundle.ConfigBuilder(shiftplanJsonFile).build();
        }

        ShiftPlanRunner shiftPlanRunner = new ShiftPlanRunner();

        if (createStafflist) {
            shiftPlanRunner.createStaffList(shiftPlanCopyXMLFile, xmlPath, stafflistDir);
            return;
        }

        SwapParams swapParams;
        try {
            if (swapParamsString != null && !swapParamsString.isEmpty()) {
                swapParams = shiftPlanRunner.getOperationalParams(swapParamsString);
            } else if (swapParamsFile != null) {
                swapParams = shiftPlanRunner.getOperationalParams(swapParamsFile);
            } else {
                swapParams = shiftPlanRunner.getOperationalParams();
            }

            if (swapParams.getMode() == OP_MODE.CREATE) {
                shiftPlanRunner.createShiftPlan(xmlPath, shiftPlanCopyXMLFile, swapParams);
            } else {
                shiftPlanRunner.modifyShiftPlan(shiftPlanCopyXMLFile, xmlPath, swapParams);
            }

            Map<String, Object> dataModel = shiftPlanRunner.getShiftplanCopy(shiftPlanCopyXMLFile, xmlPath);
            Path attachment = shiftPlanRunner.createPDF(templatePath, dataModel, generatedData);
            logger.info("Neuer Schichtplan in '{}' gespeichert", attachment.toString());

            if (sendMail) {
                if (password != null && !password.isBlank()) {
                    Employee[] employees = (Employee[]) dataModel.get("employees");
                    EmailDispatch emailDispatch = new EmailDispatch(mailConfigPath);
                    emailDispatch.sendMail(List.of(employees), attachment, password);
                }
            }
        } catch (IOException ex) {
            logger.fatal("XML/XSD-Datei kann nicht gelesen oder geschrieben werden", ex);
        } catch (TemplateException ex) {
            logger.fatal("Das FTL-Template kann nicht verarbeitet werden", ex);
        } catch (EmailException ex) {
            logger.error("Der Emailversand des Schichtplans ist gescheitert", ex);
        } catch (ShiftPlanRunnerException | InvalidShiftPlanException | ShiftPlanSwapException ex) {
            logger.fatal(ex.getMessage());
        } catch (RuntimeException ex) {
            logger.fatal("Fehler bei Ausführung des Programms", ex);
        }
    }
}
