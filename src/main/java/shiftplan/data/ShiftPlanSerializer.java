package shiftplan.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import shiftplan.calendar.Shift;
import shiftplan.calendar.ShiftPlanCopy;
import shiftplan.calendar.ShiftPolicy;
import shiftplan.calendar.ShiftSwapDataModelConverter;
import shiftplan.data.xml.DocumentParser;
import shiftplan.users.Employee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;

public class ShiftPlanSerializer {

    private static final DateTimeFormatter isoDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Logger logger = LogManager.getLogger(ShiftPlanSerializer.class);

    private Path xmlFile;
    private Path xsdFile;

    /**
     * ShiftPlanSerializer in Write-Only - Modus.

     * Der Target - OutputStream wird als Parameter bei aufruf von
     * <code>serializer.writeXML(Document doc, OutputStream out)</code> angegeben
     */
    public ShiftPlanSerializer() {}

    /**
     * ShiftPlanSerializer in Write-Only - Modus
     *
     * @param xmlFile Pfad zur XML-Datei, in die die Objekt-Repräsentation des Schichtplans geschrieben wird
     */
    public ShiftPlanSerializer(String xmlFile) {
        this(Path.of(xmlFile));
    }

    /**
     * ShiftPlanSerializer in Read-/Write - Modus
     *
     * @param xmlFile Pfad zur XML-Datei, in die die Objekt-Repräsentation des Schichtplans geschrieben wird
     * @param xsdDir Pfad zum Verzeichnis, in welchem sich die XML-Schema - Datei 'shiftplan_serialized.xsd' befindet,
     *               die zur Validierung der XML - Datendatei (shiftplan_serialized.xml) verwendet wird.
     */
    public ShiftPlanSerializer(String xmlFile, String xsdDir) {
        this(Path.of(xmlFile), Path.of(xsdDir));
    }


    /**
     * ShiftPlanSerializer in Write-Only - Modus
     *
     * @param xmlFile Pfad zur XML-Datei, in die die Objekt-Repräsentation des Schichtplans geschrieben wird
     */
    public ShiftPlanSerializer(Path xmlFile) {
        Objects.requireNonNull(xmlFile, "Serialisierter Schichtplan (shiftplan_serialized.xml) fehlt!");
        this.xmlFile = xmlFile;
    }

    /**
     * ShiftPlanSerializer in Read-/Write - Modus
     *
     * @param xmlFile Pfad zur XML-Datei, in die die Objekt-Repräsentation des Schichtplans geschrieben wird
     * @param xsdDir Pfad zum Verzeichnis, in welchem sich die XML-Schema - Datei 'shiftplan_serialized.xsd' befindet,
     *               die zur Validierung der XML - Datendatei (shiftplan_serialized.xml) verwendet wird.
     */
    public ShiftPlanSerializer(Path xmlFile, Path xsdDir) {
        Objects.requireNonNull(xmlFile, "Serialisierter Schichtplan (shiftplan_serialized.xml) fehlt!");
        Objects.requireNonNull(xsdDir, "XML-Verzeichnis fehlt!");

        Path xsdFile = xsdDir.resolve("shiftplan_serialized.xsd");
        this.xmlFile = xmlFile;
        this.xsdFile = xsdFile;
    }

    private boolean isValidFile(Path file) throws IllegalArgumentException {
        if (!Files.exists(file) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException("Ungültige oder nicht existierende Xml|XSD-Datei: " + file + "!");
        }
        return true;
    }

    public Document serializeShiftPlan(ShiftSwapDataModelConverter converter) {
        return serializeShiftPlan(
                converter.getYear(),
                converter.getStartDate(),
                converter.getEndDate(),
                converter.getShiftPlan(),
                converter.getCalendar(),
                converter.getEmployees()
        );
    }

    public Document serializeShiftPlan(int year, LocalDate startDate, LocalDate endDate, Map<String, Shift> shiftPlan,
                                       Map<Integer, LocalDate[]> calendar, Employee[] employeesArray) {

        ShiftPolicy policy = ShiftPolicy.INSTANCE;
        List<DayOfWeek> noLateShiftOnWeekdays = policy.getNoLateShiftOn();

        Document doc = new Document();
        Element root = new Element("shiftplan");
        root.setAttribute("for", String.valueOf(year));
        root.setAttribute("start", isoDate.format(startDate));
        root.setAttribute("end", isoDate.format(endDate));
        doc.setRootElement(root);

        Element creationParams = new Element("creation-params");

        Element lateShiftPeriod = new Element("late-shift-period");
        lateShiftPeriod.setText(String.valueOf(policy.getLateShiftPeriod()));
        lateShiftPeriod.addContent(new Comment("Anzahl der fortlaufenden Spätschichttage"));
        creationParams.addContent(lateShiftPeriod);

        Element maxHomePerMonth = new Element("max-home-per-month");
        maxHomePerMonth.setText(String.valueOf(policy.getMaxHoDaysPerMonth()));
        maxHomePerMonth.addContent(new Comment("Maximale Anzahl von Homeoffice-Tagen pro Monat"));
        creationParams.addContent(maxHomePerMonth);

        Element hoCreditsPerEmployee = new Element("ho-credits-per-employee");
        hoCreditsPerEmployee.setText(String.valueOf(policy.getWeeklyHoCreditsPerEmployee()));
        hoCreditsPerEmployee.addContent(new Comment("Max. Anzahl von HO-Tagen pro Woche"));
        creationParams.addContent(hoCreditsPerEmployee);

        Element maxHoSlotsPerDay = new Element("max-ho-slots-per-day");
        maxHoSlotsPerDay.setText(String.valueOf(policy.getMaxHoSlots()));
        maxHoSlotsPerDay.addContent(new Comment("Maximale Anzahl von MA's, die an einem Tag von zu Hause arbeiten"));
        creationParams.addContent(maxHoSlotsPerDay);

        Element maxSuccessiveHODays = new Element("max-successive-ho-days");
        maxSuccessiveHODays.setText(String.valueOf(policy.getMaxSuccessiveHODays()));
        maxSuccessiveHODays.addContent(new Comment("Maximale Anzahl aufeinanderfolgender HO-Tage eines MA's"));
        creationParams.addContent(maxSuccessiveHODays);

        Element minDistanceBetweenHOBlocks = new Element("min-distance-between-ho-blocks");
        minDistanceBetweenHOBlocks.setText(String.valueOf(policy.getMinDistanceBetweenHOBlocks()));
        minDistanceBetweenHOBlocks.addContent(new Comment(
                "Mindestanzahl von Tagen, für die ein MA nach einer festgelegten Anzahl von aufeinanderfolgenden HO-Tagen für\n" +
                        "eine weitere HO-Einteilung gesperrt ist"));
        creationParams.addContent(minDistanceBetweenHOBlocks);

        policy.getNoLateShiftOn().forEach(dayOfWeek -> {
            Element noLateShiftOn = new Element("no-lateshift-on");
            noLateShiftOn.setText(dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US).toUpperCase(Locale.ROOT));
            creationParams.addContent(noLateShiftOn);
        });

        root.addContent(creationParams);

        Element employees = new Element("employees");
        root.addContent(employees);

        for (Employee staff : employeesArray) {
            Element employee = new Element("employee");
            employee.setAttribute("id", staff.getId());
            employees.addContent(employee);

            Element name = new Element("name");
            name.setText(staff.getName());
            employee.addContent(name);

            Element lastName = new Element("lastname");
            lastName.setText(staff.getLastName());
            employee.addContent(lastName);

            Employee.PARTICIPATION_SCHEMA schema = staff.getParticipationSchema();
            String schemaValue = schema == null ? "HO_LS" : schema.toString();
            Element participation = new Element("participation");
            participation.setText(schemaValue);
            employee.addContent(participation);

            Element color = new Element("color");
            color.setText(staff.getHighlightColor());
            employee.addContent(color);

            String optionalMail = staff.getEmail();
            if (optionalMail != null && !optionalMail.isEmpty()) {
                Element email = new Element("email");
                email.setText(Objects.requireNonNullElse(staff.getEmail(), ""));
                employee.addContent(email);
            }

            List<Employee> backupStaff = staff.getBackups();
            if (!backupStaff.isEmpty()) {
                Element backups = new Element("backups");
                employee.addContent(backups);

                backupStaff.forEach(baStaff -> {
                    Element backup = new Element("backup");
                    backup.setAttribute("idref", baStaff.getId());
                    backups.addContent(backup);
                });
            }
        }

        Element calendarWeeks = new Element("calendar-weeks");
        root.addContent(calendarWeeks);

        for (Map.Entry<Integer, LocalDate[]> entry : calendar.entrySet()) { // Schichtplan wochenweise durchlaufen
            Integer cwIndex = entry.getKey();
            LocalDate[] cw = entry.getValue();

            assert cw != null;

            Element calendarWeek = new Element("calendar-week");
            calendarWeek.setAttribute("index", cwIndex.toString());
            if (cw.length > 0) {
                calendarWeek.setAttribute("from", isoDate.format(cw[0]));
                calendarWeek.setAttribute("to", isoDate.format(cw[cw.length - 1]));
            }
            calendarWeeks.addContent(calendarWeek);

            for (LocalDate date : cw) {
                Shift shift = shiftPlan.get(date.toString());
                if (shift == null) continue;

                Element day = new Element("day");
                day.setAttribute("name",
                        date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US).toUpperCase(Locale.ROOT));
                day.setAttribute("date", isoDate.format(date));
                day.setAttribute("is-lateshift", noLateShiftOnWeekdays.contains(date.getDayOfWeek()) ? "false" : "true");
                calendarWeek.addContent(day);

                Employee ls = shift.getLateShift();
                if (ls != null) {
                    Element lateShift = new Element("late-shift");
                    day.addContent(lateShift);

                    Element employee = new Element("employee");
                    employee.setAttribute("idref", ls.getId());
                    lateShift.addContent(employee);
                }

                Employee[] employeesInHo = shift.getEmployeesInHo();
                if (Arrays.stream(employeesInHo).allMatch(Objects::isNull)) continue;

                Element hoOffice = new Element("ho-office");
                day.addContent(hoOffice);
                for (Employee emp : shift.getEmployeesInHo()) {
                    if (emp != null) {
                        Element employee = new Element("employee");
                        employee.setAttribute("idref", emp.getId());
                        hoOffice.addContent(employee);
                    }
                }
            }
        }

        return doc;
    }

    public ShiftPlanCopy deserializeShiftplan() throws IOException, JDOMException {
        // Die Dateien shiftplan_serialized.xml und shiftplan_serialized.xsd müssen existieren und gelesen werden können.
        boolean isValidXML = isValidFile(xmlFile);
        boolean isValidXSD = isValidFile(xsdFile);
        logger.info("shiftplan_serialized_xml und shiftplan_serialized.xsd existieren und sind lesbar: {}, {}",
                isValidXML, isValidXSD);

        DocumentParser documentParser = new DocumentParser(xmlFile, xsdFile);
        Document doc = documentParser.getXMLDocument();

        Element root = doc.getRootElement();
        logger.info("Datenquelle 'shiftplan_serialized.xml' wird gelesen");

        int year = Integer.parseInt(root.getAttributeValue("for"));

        logger.info("Auslesen des Startdatums ...");
        LocalDate startDate;
        try {
            Attribute startDateAttr = Objects.requireNonNull(root.getAttribute("start"),
                    "Startdatum für Schichtplan fehlt!");
            startDate = LocalDate.parse(startDateAttr.getValue());
        } catch (NullPointerException | DateTimeParseException exception) {
            throw new InvalidShiftPlanException(exception.getMessage());
        }

        logger.info("Auslesen des Enddatums");
        LocalDate endDate;
        try {
            Attribute endDateAttr = Objects.requireNonNull(root.getAttribute("end"),
                    "Enddatum für Schichtplan fehlt!");
            // XML-Datentyp: gYearMonth (xxxx-xx)
           endDate = LocalDate.parse(endDateAttr.getValue());
        } catch (NullPointerException | DateTimeParseException exception) {
            throw new InvalidShiftPlanException(exception.getMessage());
        }

        logger.info("Auslesen der Angestellten-Daten ...");
        Employee[] employees = documentParser.parseEmployeesNode(root.getChild("employees"));

        ShiftPlanCopy shiftPlanCopy = new ShiftPlanCopy(year, startDate, endDate, employees);

        logger.info("Auslesen der Spätschicht- und Homeoffice-Richtlinien ...");
        Element creationParams = root.getChild("creation-params");
        documentParser.parsePolicy(creationParams);


        logger.info("Auslesen des Schichtplans ...");
        Element calendarWeeks = root.getChild("calendar-weeks");
        calendarWeeks.getChildren("calendar-week").forEach(cw -> {
            int cwIndex = Integer.parseInt(cw.getAttributeValue("index"));
            LocalDate from = LocalDate.parse(cw.getAttributeValue("from"));
            LocalDate to = LocalDate.parse(cw.getAttributeValue("to"));
            ShiftPlanCopy.CalendarWeek calendarWeek = new ShiftPlanCopy.CalendarWeek(
                    cwIndex,
                    ShiftPlanCopy.CalendarWeek.createCalendarWeekArray(from, to));

            List<ShiftPlanCopy.WorkDay> workDays = new ArrayList<>();

            cw.getChildren("day").forEach(day -> {
                DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.getAttributeValue("name").toUpperCase(Locale.ROOT));
                LocalDate date = LocalDate.parse(day.getAttributeValue("date"));
                boolean isLateShift = Boolean.parseBoolean(day.getAttributeValue("is-lateshift"));

                ShiftPlanCopy.WorkDay workDay = new ShiftPlanCopy.WorkDay(dayOfWeek, date, isLateShift);

                if (isLateShift) {
                    Element lateShift = day.getChild("late-shift");
                    String employeeId  = lateShift.getChild("employee").getAttributeValue("idref");
                    workDay.setLateshift(shiftPlanCopy.getEmployeeById(employeeId));
                }

                Element homeOffice = day.getChild("ho-office");
                if (homeOffice != null) {
                    homeOffice.getChildren("employee").forEach(emp -> {
                        String employeeId = emp.getAttributeValue("idref");
                        workDay.addEmployeeInHo(shiftPlanCopy.getEmployeeById(employeeId));
                    });
                }
                workDays.add(workDay);
            });
            shiftPlanCopy.addCalendarWeek(calendarWeek, workDays);
        });

        logger.info("Schichtplan komplett ausgelesen");

        return shiftPlanCopy;
    }

    public void writeXML(Document doc, OutputStream out) throws IOException {
        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat());
        xmlOutputter.output(doc, out);
    }

    public void writeXML(Document doc) throws IOException {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
                xmlFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)
        ) {
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.setFormat(Format.getPrettyFormat());
            xmlOutputter.output(doc, bufferedWriter);
        }
    }
}
