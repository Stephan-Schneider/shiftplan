package shiftplan.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderXSDFactory;
import shiftplan.calendar.ShiftPolicy;
import shiftplan.users.Employee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentParser {

    private static final Logger logger = LogManager.getLogger(DocumentParser.class);

    private int year;
    private LocalDate startDate;
    private LocalDate endDate;
    private final List<LocalDate> holidays = new ArrayList<>();
    private Employee[] employees;

    private final Document doc;

    public DocumentParser() throws IOException, JDOMException, IllegalArgumentException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("shiftplan.xml");
        if (in == null) throw new IllegalArgumentException("shiftplan.xml nicht gefunden!");

        SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory());
        doc = saxBuilder.build(in);
    }

    public DocumentParser(String xmlPath, String xsdPath) throws IOException, JDOMException, IllegalArgumentException {
        Path xmlPathObj = Path.of(xmlPath);
        Path xsdPathObj = Path.of(xsdPath);
        if (Files.isRegularFile(xmlPathObj) && Files.isReadable(xmlPathObj)
                && Files.isRegularFile(xsdPathObj) && Files.isReadable(xsdPathObj)) {
            logger.debug("XMLDatei und XSD-Datei existieren und sind lesbar");

            SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory(xsdPath));
            doc = saxBuilder.build(xmlPathObj.toFile());
        } else {
            throw new IllegalArgumentException(xmlPath + " nicht gefunden");
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

    public int getYear() {
        return year;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    public Employee[] getEmployees() {
        return employees;
    }

    public void parseDocument() {
        logger.info("Datenquelle 'shiftplan.xml' wird gelesen");
        Element rootNode = doc.getRootElement();

        // XML-Datentyp: gYear
        year = Integer.parseInt(rootNode.getAttributeValue("for"));
        logger.info("Gültigkeit des Plans für das Jahr {}", year);

        logger.info("Start-Monat in {} wird ermittelt", year);
        // XML-Datentyp: gYearMonth (xxxx-xx)
        String startDateValue = rootNode.getAttributeValue("start-date");
        if (startDateValue != null) {
            String[] yearMonth = startDateValue.split("-");
            int yearPart = Integer.parseInt(yearMonth[0]);
            int monthPart = Integer.parseInt(yearMonth[1]);
            // Das Anfangsdatum muss sich innerhalb des im root-Element angegebenen Jahres befinden
            if (yearPart == year) {
                startDate = LocalDate.of(yearPart, monthPart, 1);
            }
        }
        // Entweder das notierte Startdatum des Schichtplans oder das Default-Datum 01.01.20xx
        logger.info("Start-Datum: {}", startDate != null ? startDate : LocalDate.of(year, 1, 1));

        logger.info("Enddatum in {} wird ermittelt", year);
        // XML-Datentyp: gYearMonth (xxxx-xx)
        String endDateValue = rootNode.getAttributeValue("end-date");
        if (endDateValue != null) {
            String[] yearMonth = endDateValue.split("-");
            int yearPart = Integer.parseInt(yearMonth[0]);
            int monthPart = Integer.parseInt(yearMonth[1]);
            // Das Enddatum muss sich innerhalb des im root-Element angegebenen Jahres befinden
            if (yearPart == year) {
                // Willkürlicher Tag um LocalDate-Objekt des durch <end-date> angegebenen Monat/Jahr zu erhalten
                LocalDate temp = LocalDate.of(yearPart, monthPart, 1);
                endDate = LocalDate.of(temp.getYear(), temp.getMonthValue(), temp.lengthOfMonth());
            }
        }
        // Entweder das notierte Enddatum des Schichtplans oder das Default-Datum 31.12.20xx
        logger.info("End-Datum: {}", endDate != null ? endDate : LocalDate.of(year, 12, 31));

        Element publicHolidays = rootNode.getChild("public-holidays");
        if (publicHolidays != null) {
            publicHolidays.getChildren("holiday").forEach(this::parseHolidayNode);
            logger.debug("holidays: {}", holidays);
            logger.info("{} eingetragene öffentliche Feiertage geparst", holidays.size());
        } else {
            logger.info("Keine Feiertage hinterlegt - der Schichtplan wird ohne Berücksichtigung von Feiertagen erstellt");
        }

        logger.info("Dauer der Home-Office- und Spätschicht-Phasen werden ausgelesen");
        ShiftPolicy policy = ShiftPolicy.INSTANCE;
        ShiftPolicy.Builder builder = new ShiftPolicy.Builder();

        Element shiftPolicy = rootNode.getChild("shift-policy");
        builder.setLateShiftPeriod(Integer.parseInt(shiftPolicy.getChildText("late-shift-period")));
        shiftPolicy
                .getChildren("no-lateshift-on")
                .forEach(element -> builder.addNoLateShiftOn(element.getText()));
        builder.setMaxHoDaysPerMonth(Integer.parseInt(shiftPolicy.getChildText("max-home-per-month")));
        builder.setWeeklyHoCreditsPerEmployee(Integer.parseInt(shiftPolicy.getChildText("ho-credits-per-employee")));
        builder.setMaxHoSlots(Integer.parseInt(shiftPolicy.getChildText("max-ho-slots-per-day")));

        policy.createShiftPolicy(builder);

        logger.info("Dauer der Spätschicht-Phase: {} / Maximale Anzahl von HO-Tagen pro Woche: {} / " +
                        "Maximale Anzahl von MA's im Homeoffice pro Arbeitstag: {}",
                policy.getLateShiftPeriod(), policy.getWeeklyHoCreditsPerEmployee(), policy.getMaxHoSlots());

        logger.info("Liste der Angestellten wird ausgelesen ...");
        List<Employee> employeeList = new ArrayList<>();
        Map<Employee, List<String>> tmpBackupMap = new HashMap<>(); // Mapping zwischen MA's und deren Backups
        rootNode.getChild("employees").getChildren("employee").forEach(employeeNode -> {

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

        this.employees = employeeList.toArray(new Employee[0]);

        logger.info("Angestelltenliste ausgelesen ({} Mitarbeiter)", this.employees.length);
    }

    private void parseHolidayNode(Element holidayNode) {
        String date = holidayNode.getAttributeValue("date");

        try {
            LocalDate holiday = LocalDate.parse(date);
            if (!(holiday.getYear() == this.year)) {
                throw new IllegalArgumentException("Nur Feiertage im Jahr " + this.year + " zulässig!");
            }
            holidays.add(holiday);
        } catch (DateTimeParseException ex) {
            logger.error("Malformed date " + date, ex);
            throw ex;
        }
    }
}
