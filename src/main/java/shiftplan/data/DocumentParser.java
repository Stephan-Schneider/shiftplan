package shiftplan.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderXSDFactory;
import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class DocumentParser {

    private static final Logger logger = LogManager.getLogger(DocumentParser.class);

    private int year;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxHomeOfficeDaysPerWeek;
    private int maxHomeOfficeDaysPerMonth;
    private int homeOfficeDuration;
    private int maxLateShiftDuration;
    private final List<LocalDate> holidays = new ArrayList<>();
    private final List<EmployeeGroup> employeeGroupList = new ArrayList<>();

    private final Document doc;

    public DocumentParser() throws IOException, JDOMException, IllegalArgumentException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("shiftplan.xml");
        if (in == null) throw new IllegalArgumentException("shiftplan.xml nicht gefunden!");

        SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory());
        doc = saxBuilder.build(in);
    }

    public DocumentParser(String xmlPath, String xsdPath) throws IOException, JDOMException, IllegalArgumentException {
        Path path = Path.of(xmlPath);
        if (Files.isRegularFile(path) && Files.isReadable(path)) {
            SAXBuilder saxBuilder = new SAXBuilder(getSchemaFactory(xsdPath));
            doc = saxBuilder.build(path.toFile());
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
        URL url = this.getClass().getClassLoader().getResource(xsdPath);
        if (url == null) {
            throw new IllegalArgumentException(xsdPath + " nicht gefunden!");
        }
        File schemaFile = new File(url.getFile());
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

    public int getMaxHomeOfficeDaysPerWeek() {
        return maxHomeOfficeDaysPerWeek;
    }

    public int getMaxHomeOfficeDaysPerMonth() {
        return maxHomeOfficeDaysPerMonth;
    }

    public int getHomeOfficeDuration() {
        return homeOfficeDuration;
    }

    public int getMaxLateShiftDuration() {
        return maxLateShiftDuration;
    }

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    public List<EmployeeGroup> getEmployeeGroupList() {
        return employeeGroupList;
    }

    public void parseDocument() {
        logger.info("Datenquelle 'shiftplan.xml' wird gelesen");
        Element rootNode = doc.getRootElement();
        year = Integer.parseInt(rootNode.getAttributeValue("for"));
        logger.info("Gültigkeit des Plans für das Jahr {}", year);

        logger.info("Startdatum in {} wird ermittelt", year);
        String startDateValue = rootNode.getAttributeValue("start-date");
        if (startDateValue != null && !startDateValue.isBlank()) {
            LocalDate startDateTemp = LocalDate.parse(startDateValue);
            if (startDateTemp.getYear() == year) {
                // Das Anfangsdatum muss sich innerhalb des im root-Element angegebenen Jahres befinden
                startDate = startDateTemp;
            }
        }
        // Entweder das notierte Startdatum des Schichtplans oder das Default-Datum 01.01.20xx
        logger.info("Start-Datum: {}", startDate != null ? startDate : LocalDate.of(year, 1, 1));

        logger.info("Enddatum in {} wird ermittelt", year);
        String endDateValue = rootNode.getAttributeValue("end-date");
        if (endDateValue != null && !endDateValue.isBlank()) {
            LocalDate endDateTemp = LocalDate.parse(endDateValue);
            if (endDateTemp.getYear() == year) {
                // Das Enddatum muss sich innerhalb des im root-Element angegebenen Jahres befinden
                endDate = endDateTemp;
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
        Element shiftDuration = rootNode.getChild("shift-duration");
        maxHomeOfficeDaysPerWeek = Integer.parseInt(shiftDuration.getChildText("max-home-per-week"));
        maxHomeOfficeDaysPerMonth = Integer.parseInt(shiftDuration.getChildText("max-home-per-month"));
        homeOfficeDuration = Integer.parseInt(shiftDuration.getChildText("home-office"));
        maxLateShiftDuration = Integer.parseInt(shiftDuration.getChildText("max-late-shift"));
        logger.info("Dauer der Homeoffice-Phase: {} / Maximale Dauer der Spätschicht-Phase: {}",
                homeOfficeDuration, maxLateShiftDuration);

        logger.info("Schichtgruppen werden ausgelesen ...");
        List<Element> employeeGroups = rootNode.getChild("employee-groups").getChildren("employee-group");
        for (Element employeeGroup : employeeGroups) {
            EmployeeGroup empGroup;
            List<Employee> employees = new ArrayList<>();

            String groupName = employeeGroup.getChildText("group-name");
            logger.info("Schichtgruppe {} wird geparsed", groupName);

            employeeGroup.getChild("employees").getChildren("employee").forEach(employeeNode -> {
                logger.info("Angestellte der Gruppe {} werden verarbeitet", groupName);
                Employee employee;
                String empName = employeeNode.getChildText("name");
                String empLastName = employeeNode.getChildText("lastname");
                int empShiftOrder = Integer.parseInt(employeeNode.getChildText("lateshift-order"));
                boolean empLateShiftOnly = Boolean.parseBoolean(employeeNode.getChildText("lateshift-only"));
                if (employeeNode.getChild("color") != null) {
                    String color = employeeNode.getChildText("color");
                    employee = new Employee(empName, empLastName, empShiftOrder, empLateShiftOnly, color);
                } else {
                    employee = new Employee(empName, empLastName, empShiftOrder, empLateShiftOnly);
                }
                logger.info("Employee {} wird Gruppe {} hinzugefügt", employee, groupName);
                employees.add(employee);
            });
            Employee[] empArray = employees.toArray(new Employee[0]);
            empGroup = new EmployeeGroup(groupName, empArray);
            employeeGroupList.add(empGroup);
        }
        logger.info("Alle Schichtgruppen verarbeitet ({} Gruppen)", employeeGroupList.size());
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
