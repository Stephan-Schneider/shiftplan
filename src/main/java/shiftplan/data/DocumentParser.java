package shiftplan.data;

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
import shiftplan.users.Employee;

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

    public DocumentParser() throws IOException, JDOMException, ShiftPlanRunnerException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("shiftplan.xml");
        if (in == null) throw new ShiftPlanRunnerException("shiftplan.xml nicht gefunden!");

        SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory());
        doc = saxBuilder.build(in);
    }

    public DocumentParser(String xmlPath, String xsdPath) throws IOException, JDOMException, IllegalArgumentException {
        this(Path.of(xmlPath), Path.of(xsdPath));
    }

    public DocumentParser(Path xmlPath, Path xsdPath) throws IOException, JDOMException, ShiftPlanRunnerException {
        if (Files.isRegularFile(xmlPath) && Files.isReadable(xmlPath)
                && Files.isRegularFile(xsdPath) && Files.isReadable(xsdPath)) {
            logger.debug("XMLDatei und XSD-Datei existieren und sind lesbar");

            SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory(String.valueOf(xsdPath)));
            //SAXBuilder saxBuilder = new SAXBuilder();
            doc = saxBuilder.build(xmlPath.toFile());
        } else {
            throw new ShiftPlanRunnerException(xmlPath + " nicht gefunden");
        }
    }

    private XMLReaderJDOMFactory getSchemaFactory() throws JDOMException {
        return getSchemaFactory("shiftplan.xsd");
    }

    private XMLReaderJDOMFactory getSchemaFactory(String xsdPath) throws JDOMException {
        // Die Schema-Datei kann nur lokalisiert werden, wenn:
        // A) die Anwendung nicht in ein JAR-Archiv gepackt ist (d.h. die Anwendung liegt in "explodierter" Form vor)
        // B) die Anwendung in ein JAR-Archiv gepackt ist und der Pfad zu einer außerhalb des Archivs liegenden
        //    Schema-Datei übergeben wird.
        File schemaFile;
        URL url = this.getClass().getClassLoader().getResource(xsdPath);
        // Testen, ob die XSD-Datei als Resource geladen werden kann
        if (url != null) {
            schemaFile = new File(url.getFile());
            return new XMLReaderXSDFactory(schemaFile);
        }
        // Falls die Resource mit der o.a. Methode nicht gefunden wird, ist davon auszugehen, dass es sich bei
        // <xsdPath> um den absoluten Pfad zu einer externen Resource handelt. Es wird ein Path-/File-Objekt erstellt.
        schemaFile = Path.of(xsdPath).toFile();
        return new XMLReaderXSDFactory(schemaFile);
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

        for (Employee key : tmpBackupMap.keySet()) {
            List<String> ids = tmpBackupMap.get(key);
            List<Employee> backupsForKey = employeeList.stream().filter(employee -> ids.contains(employee.getId())).toList();
            key.addBackups(backupsForKey);
            logger.info("MA {} {} mit diesen Backups erstellt: {}", key.getName(), key.getLastName(), key.getBackups());
        }

        return employeeList.toArray(new Employee[0]);
    }
}
