package shiftplan.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import shiftplan.ShiftPlanRunnerException;
import shiftplan.users.Employee;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ShiftPlanDescriptor {

    private static final Logger logger = LogManager.getLogger(ShiftPlanDescriptor.class);

    private int year;
    private LocalDate startDate;
    private LocalDate endDate;
    private final List<LocalDate> holidays = new ArrayList<>();
    private Employee[] employees;

    private Path xmlFile;
    private Path xsdFile;

    public ShiftPlanDescriptor() {}

    public ShiftPlanDescriptor(Path xmlDir) {
        try {
            Objects.requireNonNull(xmlDir, "XML-Verzeichnis fehlt!");
        } catch (NullPointerException ex) {
            throw new ShiftPlanRunnerException(ex.getMessage());
        }

        Path xmlFile = xmlDir.resolve("shiftplan.xml");
        Path xsdFile = xmlDir.resolve("shiftplan.xsd");

        if (Files.isRegularFile(xmlFile) && Files.isReadable(xmlFile)
                && Files.isRegularFile(xsdFile) && Files.isReadable(xsdFile)) {
            this.xmlFile = xmlFile;
            this.xsdFile = xsdFile;
        } else {
            throw new ShiftPlanRunnerException("Ungültiger XML-Pfad!");
        }
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

    public void parseDocument() throws InvalidShiftPlanException, IOException, JDOMException {
        logger.info("Datenquelle 'shiftplan.xml' wird gelesen");

        DocumentParser parser;
        if (xmlFile == null || xsdFile == null) {
            parser = new DocumentParser();
        } else {
            parser = new DocumentParser(xmlFile, xsdFile);
        }
        Document doc = parser.getXMLDocument();
        Element rootNode = doc.getRootElement();

        // XML-Datentyp: gYear
        year = Integer.parseInt(rootNode.getAttributeValue("for"));
        logger.info("Gültigkeit des Plans für das Jahr {}", year);

        logger.info("Start-Monat in {} wird ermittelt", year);
        Attribute startDateAttr = rootNode.getAttribute("start-date");
        if (startDateAttr != null) {
            startDate = parser.parseShiftPlanDate(startDateAttr);
            if (startDate.getYear() != year) {
                // Das Startdatum des Schichtplans muss in dem Jahr liegen, das im for-Attribute angegeben ist
                throw new InvalidShiftPlanException("Ungültiges Startdatum. Grund: Ungültige Jahresangabe°!");
            }
        }

        logger.info("Enddatum in {} wird ermittelt", year);
        // XML-Datentyp: gYearMonth (xxxx-xx)
        Attribute endDateAttr = rootNode.getAttribute("end-date");
        if (endDateAttr != null) {
            endDate = parser.parseShiftPlanDate(endDateAttr);
            if (endDate.getYear() != year) {
                // Das Enddatum des Schichtplans muss in dem Jahr liegen, das im for-Attribute angegeben ist
                throw new InvalidShiftPlanException("Ungültiges Enddatum. Grund: Ungültige Jahresangabe°!");
            }
        }

        Element publicHolidays = rootNode.getChild("public-holidays");
        if (publicHolidays != null) {
            publicHolidays.getChildren("holiday").forEach(this::parseHolidayNode);
            logger.debug("holidays: {}", holidays);
            logger.info("{} eingetragene öffentliche Feiertage geparst", holidays.size());
        } else {
            logger.info("Keine Feiertage hinterlegt - der Schichtplan wird ohne Berücksichtigung von Feiertagen erstellt");
        }

        logger.info("Auslesen der Spätschicht- und Homeoffice-Richtlinien ...");
        Element shiftPolicy = rootNode.getChild("shift-policy");
        parser.parsePolicy(shiftPolicy);

        logger.info("Liste der Angestellten wird ausgelesen ...");
        this.employees = parser.parseEmployeesNode(rootNode.getChild("employees"));
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
