package shiftplan.data.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderXSDFactory;
import shiftplan.ShiftPlanRunnerException;
import shiftplan.calendar.ShiftPolicy;
import shiftplan.data.IShiftplanDescriptor;
import shiftplan.users.Employee;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public class DocumentParser {

    private static final Logger logger = LogManager.getLogger(DocumentParser.class);

    private final Document doc;

    /**
     * Constructs a new instance of the DocumentParser class.
     * This constructor attempts to load and parse the default "shiftplan.xml" file
     * located in the application's classpath. The XML file is validated against the "shiftplan.xsd" schema.
     *
     * @throws ShiftPlanRunnerException if the "shiftplan.xml" file cannot be found in the classpath.
     * @throws IOException if an I/O error occurs while reading the XML file.
     * @throws JDOMException if there are issues parsing the XML document.
     */
    public DocumentParser() throws IOException, JDOMException, ShiftPlanRunnerException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("shiftplan.xml");
        if (in == null) throw new ShiftPlanRunnerException("shiftplan.xml nicht gefunden!");

        SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory("shiftplan.xsd"));
        doc = saxBuilder.build(in);
    }

    public DocumentParser(String xmlPath) throws IOException, JDOMException, ShiftPlanRunnerException {
        this(Path.of(xmlPath));
    }

    /**
     * Constructs a new instance of the DocumentParser class and parses the provided XML file.
     * The XML file is validated against a schema, determined based on the file name.
     *
     * @param xmlPath the path to the XML file to be parsed. If the file is named "shiftplan.xml",
     *                it is validated against "shiftplan.xsd". Otherwise, it is validated against
     *                "shiftplan_serialized.xsd".
     * @throws IOException if an I/O error occurs while reading the XML file.
     * @throws JDOMException if there are issues parsing the XML document.
     * @throws ShiftPlanRunnerException if the provided file does not exist or is not readable.
     */
    public DocumentParser(Path xmlPath) throws IOException, JDOMException, ShiftPlanRunnerException {
        if (Files.isRegularFile(xmlPath) && Files.isReadable(xmlPath)) {
            logger.debug("Die XMLDatei {} existiert und ist lesbar", xmlPath);

            String schemaFile;
            if (xmlPath.getFileName().toString().equals("shiftplan.xml")) {
                schemaFile = "shiftplan.xsd";
            } else {
                schemaFile = "shiftplan_serialized.xsd";
            }
            SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory(schemaFile));
            doc = saxBuilder.build(xmlPath.toFile());
        } else {
            throw new ShiftPlanRunnerException(xmlPath + " nicht gefunden");
        }
    }

    public DocumentParser(String xmlPath, String xsdPath) throws IOException, JDOMException, IllegalArgumentException {
        this(Path.of(xmlPath), Path.of(xsdPath));
    }

    /**
     * Constructs a new instance of the DocumentParser class, which parses an XML file
     * and validates it against a specified XSD schema file. The XML and XSD files
     * must both exist and be readable.
     *
     * @param xmlPath the path to the XML file to be parsed.
     * @param xsdPath the path to the XSD schema file used for validation.
     * @throws IOException if an I/O error occurs while reading the files.
     * @throws JDOMException if there are issues parsing the XML document.
     * @throws ShiftPlanRunnerException if the XML or XSD file does not exist or is not readable.
     */
    public DocumentParser(Path xmlPath, Path xsdPath) throws IOException, JDOMException, ShiftPlanRunnerException {
        if (Files.isRegularFile(xmlPath) && Files.isReadable(xmlPath)
                && Files.isRegularFile(xsdPath) && Files.isReadable(xsdPath)) {
            logger.debug("XMLDatei und XSD-Datei existieren und sind lesbar");

            SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory(xsdPath));
            //SAXBuilder saxBuilder = new SAXBuilder();
            doc = saxBuilder.build(xmlPath.toFile());
        } else {
            throw new ShiftPlanRunnerException(xmlPath + " nicht gefunden");
        }
    }

    private XMLReaderJDOMFactory getSchemaFactory(String schemaFile) throws JDOMException {
        return getSchemaFactoryFromClassPath(schemaFile);
    }

    private XMLReaderJDOMFactory getSchemaFactory(Path xsdPath) throws JDOMException {
        return new XMLReaderXSDFactory(xsdPath.toFile());
    }

    private XMLReaderJDOMFactory getSchemaFactoryFromClassPath(String xsdResource) throws ShiftPlanRunnerException, JDOMException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL schemaUrl = classLoader.getResource(xsdResource);
        InputStream schemaStream = classLoader.getResourceAsStream(xsdResource);

        if (schemaUrl == null || schemaStream == null) {
            throw new ShiftPlanRunnerException("Schema-Datei '" + xsdResource + "' nicht gefunden!");
        }

        StreamSource streamSource = new StreamSource(schemaStream);
        streamSource.setSystemId(schemaUrl.toExternalForm());

        return new XMLReaderXSDFactory(streamSource);
    }

    public Document getXMLDocument() {
        return doc;
    }

    public LocalDate parseShiftPlanDate(Attribute dateAttr) {
        assert dateAttr != null;
        // XML-Datentyp: gYearMonth (xxxx-xx)
        String dateValue = dateAttr.getValue();

        String[] yearMonth = dateValue.split("-");
        int yearPart = Integer.parseInt(yearMonth[0]);
        int monthPart = Integer.parseInt(yearMonth[1]);

        String attrName = dateAttr.getName();
        LocalDate date;
        if ("end-date".equals(attrName) || "end".equals(attrName)) {
            // Willkürlicher Tag um LocalDate-Objekt des durch <end-date> angegebenen Monat/Jahr zu erhalten
            LocalDate temp = LocalDate.of(yearPart, monthPart, 1);
            date = LocalDate.of(temp.getYear(), temp.getMonthValue(), temp.lengthOfMonth());
        } else {
            date =  LocalDate.of(yearPart, monthPart, 1);
        }

        logger.info("Start-/End-Datum: {}", date);
        return date;
    }

    public void parsePolicy(Element policyNode) {
        ShiftPolicy policy = ShiftPolicy.INSTANCE;
        ShiftPolicy.Builder builder = new ShiftPolicy.Builder();

        builder.setLateShiftPeriod(Integer.parseInt(policyNode.getChildText("late-shift-period").strip()));
        policyNode
                .getChildren("no-lateshift-on")
                .forEach(element -> builder.addNoLateShiftOn(element.getText()));
        builder.setMaxHoDaysPerMonth(Integer.parseInt(policyNode.getChildText("max-home-per-month").strip()));
        builder.setWeeklyHoCreditsPerEmployee(Integer.parseInt(policyNode.getChildText("ho-credits-per-employee").strip()));
        builder.setMaxHoSlots(Integer.parseInt(policyNode.getChildText("max-ho-slots-per-day").strip()));
        builder.setMaxSuccessiveHODays(Integer.parseInt(policyNode.getChildText("max-successive-ho-days").strip()));
        builder.setMinDistanceBetweenHOBlocks(Integer.parseInt(
                policyNode.getChildText("min-distance-between-ho-blocks").strip()));

        policy.createShiftPolicy(builder);

        logger.info("Dauer der Spätschicht-Phase: {} / Maximale Anzahl von HO-Tagen pro Woche: {} / " +
                        "Maximale Anzahl von MA's im Homeoffice pro Arbeitstag: {}",
                policy.getLateShiftPeriod(), policy.getWeeklyHoCreditsPerEmployee(), policy.getMaxHoSlots());
    }

    public Employee[] parseEmployeesNode(Element employeesNode) {
        List<Employee> employeeList = new ArrayList<>();
        Map<Employee, List<String>> tmpBackupMap = new HashMap<>(); // Mapping zwischen MA's und deren Backups
        employeesNode.getChildren("employee").forEach(employeeNode -> {

            String empId = employeeNode.getAttributeValue("id");
            String empName = employeeNode.getChildText("name");
            String empLastName = employeeNode.getChildText("lastname");
            Employee.PARTICIPATION_SCHEMA empPartSchema =
                    Employee.PARTICIPATION_SCHEMA.valueOf(employeeNode.getChildText("participation"));
            String empColor = employeeNode.getChildText("color");
            String empEmail = employeeNode.getChildText("email");

            List<String> backupIds = new ArrayList<>(); // Liste der Backup-IDs des MA's
            Element backupNode = employeeNode.getChild("backups");
            if (backupNode != null) { // <backups> ist ein optionales Element, daher muss auf null geprüft werden
                backupNode.getChildren("backup").forEach(backup -> backupIds.add(backup.getAttributeValue("idref")));
            }
            Employee employee = new Employee(empId, empName, empLastName, empPartSchema, empColor, empEmail);
            tmpBackupMap.put(employee, backupIds);
            employeeList.add(employee);
        });

        IShiftplanDescriptor.addBackups(employeeList, tmpBackupMap, logger);
        return employeeList.toArray(new Employee[0]);
    }
}
