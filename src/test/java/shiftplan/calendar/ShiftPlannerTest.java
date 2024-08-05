package shiftplan.calendar;

import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import shiftplan.data.xml.ShiftPlanDescriptor;
import shiftplan.data.ShiftPlanSerializer;
import shiftplan.document.DocGenerator;
import shiftplan.document.TemplateProcessor;
import shiftplan.users.Employee;
import shiftplan.users.HomeOfficeRecord;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ShiftPlannerTest {

    private static final Logger logger = LogManager.getLogger(ShiftPlannerTest.class);

    @Test
    void getWorkDaysForCompleteCurrentYear() throws IOException, JDOMException {
        // shiftplan.xml: start-date und end-date nicht vorhanden
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();

        LocalDate startDate = descriptor.getStartDate();
        LocalDate endDate = descriptor.getEndDate();
        List<LocalDate> holidays = descriptor.getHolidays();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, 2023, startDate, endDate);
        List<LocalDate> workdays = shiftPlanner.getWorkDays();
        LocalDate lastElement = workdays.get(workdays.size() -1);

        assertEquals(LocalDate.of(2023,1,2), workdays.get(0)); // erster Arbeitstag in 2023
        assertEquals(LocalDate.of(2023,12,29), lastElement); // letzter Arbeitstag in 2023
        assertFalse(workdays.contains(LocalDate.of(2023,2,26))); // 26.02.2023 ist Sonntag
        assertFalse(workdays.contains(LocalDate.of(2023,5,29))); // Pfingstmontag
    }

    @Test
    void getWorkDaysFromStartDateToEndOfYear() throws IOException, JDOMException {
        // shiftplan.xml: start-date = 2023-04-01, end-date: Element nicht vorhanden
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();

        LocalDate startDate = descriptor.getStartDate();
        LocalDate endDate = descriptor.getEndDate();
        List<LocalDate> holidays = descriptor.getHolidays();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, 2023, startDate,endDate);
        List<LocalDate> workdays = shiftPlanner.getWorkDays();
        LocalDate firstWorkDay = workdays.get(0);
        LocalDate lastElement = workdays.get(workdays.size() -1);

        assertEquals(LocalDate.of(2023,4,3), firstWorkDay); // erster Arbeitstag ab April 2023
        assertEquals(LocalDate.of(2023,12,29), lastElement); // letzter Arbeitstag in 2023
        assertFalse(workdays.contains(LocalDate.of(2023,12,26))); // 2. Weihnachtstag
    }

    @Test
    void getWorkDaysFromStartToEnd() throws IOException, JDOMException {
        // shiftplan.xml: start-date = 2023-04-01, end-date: 2023-09-01
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();

        LocalDate startDate = descriptor.getStartDate();
        LocalDate endDate = descriptor.getEndDate();
        List<LocalDate> holidays = descriptor.getHolidays();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, 2023, startDate, endDate);
        List<LocalDate> workdays = shiftPlanner.getWorkDays();
        LocalDate lastElement = workdays.get(workdays.size() -1);

        assertEquals(LocalDate.of(2023,4,3), workdays.get(0));
        assertEquals(LocalDate.of(2023,9,29), lastElement);
        assertFalse(workdays.contains(LocalDate.of(2023,4,16)));

    }

    @Test
    void createLateShiftPlan2() {
        ShiftPolicy policy = ShiftPolicy.INSTANCE;
        ShiftPolicy.Builder builder = new ShiftPolicy.Builder();

        builder.setLateShiftPeriod(4);
        builder.setMaxHoSlots(2);
        builder.setMaxHoDaysPerMonth(8);
        builder.setWeeklyHoCreditsPerEmployee(2);
        policy.createShiftPolicy(builder);

        Employee[] employees = getEmployees();
        LocalDate start = LocalDate.of(2023, 4,1);
        LocalDate end = LocalDate.of(2023, 7,31);
        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(new ArrayList<>(), 2023, start, end);
        Map<String, Shift> lateShifts = shiftPlanner.createLateShiftPlan(employees);

        for (String shiftDate : lateShifts.keySet()) {
            logger.debug("Spätschicht am {} wird durchgeführt von {}",
                    shiftDate, lateShifts.get(shiftDate).getLateShift().getName());
        }
    }

    @Test
    void createShiftPlan2() {
        ShiftPolicy policy = ShiftPolicy.INSTANCE;
        ShiftPolicy.Builder builder = new ShiftPolicy.Builder();

        builder.setLateShiftPeriod(4);
        builder.setMaxHoSlots(2);
        builder.setMaxHoDaysPerMonth(8);
        builder.setWeeklyHoCreditsPerEmployee(2);
        policy.createShiftPolicy(builder);

        Employee[] employees = getEmployees();
        LocalDate start = LocalDate.of(2023, 4,1);
        LocalDate end = LocalDate.of(2023, 7,31);
        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(new ArrayList<>(), 2023, start, end);
        Map<String, Shift> shiftPlan = shiftPlanner.createLateShiftPlan(employees);

        ShiftCalendar shiftCalendar = new ShiftCalendar(2023);
        Map<Integer, LocalDate[]> calendar = shiftCalendar.createCalendar(start, end);

        shiftPlanner.createHomeOfficePlan(employees, shiftPlan, calendar);

        HomeOfficeRecord.createHomeOfficeReport(employees, start, end);
        List<HomeOfficeRecord> records = HomeOfficeRecord.getAllRecords();

        for (String shiftDate : shiftPlan.keySet()) {
            logger.debug("Spätschicht am {} wird durchgeführt von {}",
                    shiftDate, shiftPlan.get(shiftDate).getLateShift().getName());
            logger.debug("Für Homeoffice eingeteilt am {}: {}", shiftDate, shiftPlan.get(shiftDate).getEmployeesInHo());
        }

        for (HomeOfficeRecord record : records) {
            logger.debug("{} in Monat {}: im Plan: {} / Nicht zugewiesen: {}",
                    record.getEmployeeName(), record.getMonth(), record.getOptionsInPlan(), record.getNotAssigned());
        }
    }

    @Test
    void createCalendar2() throws TemplateException, IOException, JDOMException {
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();

        int year = descriptor.getYear();
        LocalDate startDate = descriptor.getStartDate();
        LocalDate endDate = descriptor.getEndDate();
        List<LocalDate> holidays = descriptor.getHolidays();
        Employee[] employees = descriptor.getEmployees();

        ShiftPolicy policy = ShiftPolicy.INSTANCE;

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, year, startDate, endDate);
        Map<String, Shift> shiftPlan = shiftPlanner.createLateShiftPlan(employees);

        ShiftCalendar shiftCalendar = new ShiftCalendar(year);
        Map<Integer, LocalDate[]> calendar = shiftCalendar.createCalendar(startDate, endDate);

        shiftPlanner.createHomeOfficePlan(employees, shiftPlan, calendar);

        HomeOfficeRecord.createHomeOfficeReport(employees, startDate, endDate);
        List<HomeOfficeRecord> records = HomeOfficeRecord.getAllRecords();

        for (String shiftDate : shiftPlan.keySet()) {
            String lateShift = shiftPlan.get(shiftDate).getLateShift() == null ?
                    "Keine Spätschicht" :
                    shiftPlan.get(shiftDate).getLateShift().getName();
            logger.debug("Spätschicht am {} wird durchgeführt von {}",
                    shiftDate, lateShift);
            logger.debug("Für Homeoffice eingeteilt am {}: {}", shiftDate, shiftPlan.get(shiftDate).getEmployeesInHo());
        }

        Map<String, Integer> shiftInfo = new HashMap<>();
        shiftInfo.put("hoSlotsPerShift", policy.getMaxHoSlots());
        shiftInfo.put("hoCreditsPerWeek", policy.getWeeklyHoCreditsPerEmployee());
        shiftInfo.put("maxHoDaysPerMonth", policy.getMaxHoDaysPerMonth());
        shiftInfo.put("lateShiftDuration", policy.getLateShiftPeriod());

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("startDate", startDate);
        dataModel.put("endDate", endDate);
        dataModel.put("shiftInfo", shiftInfo);
        dataModel.put("employees", employees);
        dataModel.put("shiftPlan", shiftPlan);
        dataModel.put("calendar", calendar);
        dataModel.put("homeOfficeRecords", records);

        Path pathToXMLFile = Path.of(System.getProperty("user.home"), "shiftplan_serialized.xml");
        ShiftPlanSerializer serializer = new ShiftPlanSerializer(pathToXMLFile);
        org.jdom2.Document doc = serializer.serializeShiftPlan(
                year,
                startDate,
                endDate,
                shiftPlan,
                calendar,
                employees
        );

        serializer.writeXML(doc);

        TemplateProcessor processor = TemplateProcessor.INSTANCE;
        processor.initConfiguration();
        StringWriter output = processor.processDocumentTemplate(dataModel, "shiftplan.ftl");

        DocGenerator docGenerator = new DocGenerator();
        Document document = docGenerator.getRawHTML(output.toString());
        docGenerator.createPDF(document, Path.of(System.getProperty("user.home"), "shiftplan.pdf"));
    }

    private Employee[] getEmployees() {

        Employee emp1a = new Employee("ID-1", "Hans", "Maier", Employee.PARTICIPATION_SCHEMA.HO_LS, "#0099ee");
        Employee emp1b = new Employee("ID-2","Sabine", "Klein", Employee.PARTICIPATION_SCHEMA.HO_LS,"darkred");
        Employee emp2a = new Employee("ID-3","Willi", "Schick",Employee.PARTICIPATION_SCHEMA.HO_LS,"yellowgreen");
        Employee emp2b = new Employee("ID-4","Karla", "Meier",Employee.PARTICIPATION_SCHEMA.LS,"orangered");
        Employee emp3a = new Employee("ID-5","Otto", "Waalkes", Employee.PARTICIPATION_SCHEMA.HO_LS,"brown");
        Employee emp3b = new Employee("ID-6","Natalie", "Schön", Employee.PARTICIPATION_SCHEMA.HO_LS,"darkgoldenrod");

        emp1a.addBackup(emp1b);
        emp1b.addBackup(emp1a);

        emp2a.addBackup(emp2b);
        emp2b.addBackup(emp2a);

        emp3a.addBackup(emp3b);
        emp3b.addBackup(emp3a);

        return new Employee[] {emp1a, emp1b, emp2a, emp2b, emp3a, emp3b};
    }

    @Test
    void testDatesUntil() {
        LocalDate today = LocalDate.now();
        today.datesUntil(LocalDate.of(2023, 1,1))
                .forEach(nextDate -> logger.debug(nextDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))));
    }
}