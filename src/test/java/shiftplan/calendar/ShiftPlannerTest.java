package shiftplan.calendar;

import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import shiftplan.data.DocumentParser;
import shiftplan.document.DocGenerator;
import shiftplan.document.TemplateProcessor;
import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;
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
        DocumentParser documentParser = new DocumentParser();
        documentParser.parseDocument();

        LocalDate startDate = documentParser.getStartDate();
        LocalDate endDate = documentParser.getEndDate();
        List<LocalDate> holidays = documentParser.getHolidays();

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
        DocumentParser docParser = new DocumentParser();
        docParser.parseDocument();

        LocalDate startDate = docParser.getStartDate();
        LocalDate endDate = docParser.getEndDate();
        List<LocalDate> holidays = docParser.getHolidays();

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
        DocumentParser docParser = new DocumentParser();
        docParser.parseDocument();

        LocalDate startDate = docParser.getStartDate();
        LocalDate endDate = docParser.getEndDate();
        List<LocalDate> holidays = docParser.getHolidays();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, 2023, startDate, endDate);
        List<LocalDate> workdays = shiftPlanner.getWorkDays();
        LocalDate lastElement = workdays.get(workdays.size() -1);

        assertEquals(LocalDate.of(2023,4,3), workdays.get(0));
        assertEquals(LocalDate.of(2023,9,29), lastElement);
        assertFalse(workdays.contains(LocalDate.of(2023,4,16)));

    }

    @Test
    void createHomeOfficePlan() throws IOException, JDOMException {
        DocumentParser docParser = new DocumentParser();
        docParser.parseDocument();

        LocalDate startDate = docParser.getStartDate();
        LocalDate endDate = docParser.getEndDate();
        List<LocalDate> holidays = docParser.getHolidays();
        List<EmployeeGroup> groups = docParser.getEmployeeGroupList();
        int homeOfficeDuration = docParser.getHomeOfficeDuration();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, docParser.getYear(), startDate, endDate);
        shiftPlanner.createHomeOfficePlan(groups, homeOfficeDuration);
        assertAll("Tests",
                () -> assertTrue(groups.get(0).getHomeOfficePlan().size() > 0),
                () -> assertTrue(groups.get(1).getHomeOfficePlan().size() > 0),
                () -> assertTrue(groups.get(2).getHomeOfficePlan().size() > 0)
        );

        for (EmployeeGroup group : groups) {
            logger.debug("HomeOffice-Termine für Gruppe {}: {}", group.getGroupName(), group.getHomeOfficePlan());
        }
    }

    @Test
    void testGeneratedHOOptions() throws IOException, JDOMException {
        DocumentParser docParser = new DocumentParser();
        docParser.parseDocument();

        LocalDate startDate = docParser.getStartDate();
        LocalDate endDate = docParser.getEndDate();
        List<LocalDate> holidays = docParser.getHolidays();
        List<EmployeeGroup> groups = docParser.getEmployeeGroupList();
        int homeOfficeDuration = docParser.getHomeOfficeDuration();
        int maxPerWeek = docParser.getMaxHomeOfficeDaysPerWeek();
        int maxPerMonth = docParser.getMaxHomeOfficeDaysPerMonth();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, docParser.getYear(), startDate, endDate);
        shiftPlanner.createHomeOfficePlan(groups, homeOfficeDuration);

        for (EmployeeGroup group : groups) {
            Map<Integer, List<LocalDate>> hoDatesByMonth = group.getHomeOfficeDaysByMonth();
            hoDatesByMonth.keySet().forEach(
                    month -> logger.debug("{}: Home Office Daten in Monat {}:{}", group.getGroupName(), month,
                            hoDatesByMonth.get(month)));
            List<HomeOfficeRecord> hoRecords =
                    group.calculateHomeOfficeOptionByMonth(docParser.getYear(), maxPerWeek, maxPerMonth);
            for (HomeOfficeRecord record : hoRecords) {
                logger.debug("{}: {}", group.getGroupName(), record);
            }
        }
    }

    @Test
    void createLateShiftPlan() throws IOException, JDOMException {
        DocumentParser docParser = new DocumentParser();
        docParser.parseDocument();

        LocalDate startDate = docParser.getStartDate();
        LocalDate endDate = docParser.getEndDate();
        List<LocalDate> holidays = docParser.getHolidays();
        List<EmployeeGroup> groups = docParser.getEmployeeGroupList();
        int homeOfficeDuration = docParser.getHomeOfficeDuration();
        int lateShiftDuration = docParser.getMaxLateShiftDuration();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, docParser.getYear(), startDate, endDate);
        shiftPlanner.createHomeOfficePlan(groups, homeOfficeDuration);

        for (EmployeeGroup group : groups) {
            logger.debug("HomeOffice-Termine für Gruppe {}: {}", group.getGroupName(), group.getHomeOfficePlan());
        }

        Employee[] employees = EmployeeGroup.getEmployeesInShiftOrder();
        logger.debug("Employees: {}", Arrays.stream(employees).toList());
        shiftPlanner.createLateShiftPlan(employees, lateShiftDuration);

        /*
        for (Employee employee : employees) {
            assertTrue(employee.getLateShiftPlan().size() > 0);
            logger.debug("Late shift of {}: {}", employee.getName(), employee.getLateShiftPlan());
        } */
    }

    @Test
    void createShiftPlan() throws IOException, JDOMException {
        DocumentParser docParser = new DocumentParser();
        docParser.parseDocument();

        LocalDate startDate = docParser.getStartDate();
        LocalDate endDate = docParser.getEndDate();
        List<LocalDate> holidays = docParser.getHolidays();
        List<EmployeeGroup> groups = docParser.getEmployeeGroupList();
        int homeOfficeDuration = docParser.getHomeOfficeDuration();
        int lateShiftDuration = docParser.getMaxLateShiftDuration();

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, docParser.getYear(), startDate, endDate);

        shiftPlanner.createHomeOfficePlan(groups, homeOfficeDuration);
        Employee[]employees = EmployeeGroup.getEmployeesInShiftOrder();
        shiftPlanner.createLateShiftPlan(employees,lateShiftDuration);

        Map<String, Shift> shiftPlan = shiftPlanner.createShiftPlan(groups);
        shiftPlan.keySet().forEach(date -> {
            logger.debug(shiftPlan.get(date));
        });
        assertTrue(shiftPlan.size() > 0);

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

        Map<String, Integer> shiftInfo = new HashMap<>();
        shiftInfo.put("hoSlotsPerShift", policy.getMaxHoSlots());
        shiftInfo.put("hoCreditsPerWeek", policy.getWeeklyHoCreditsPerEmployee());
        shiftInfo.put("maxHoDaysPerMonth", policy.getMaxHoDaysPerMonth());
        shiftInfo.put("lateShiftDuration", policy.getLateShiftPeriod());

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("startDate", start);
        dataModel.put("endDate", end);
        dataModel.put("shiftInfo", shiftInfo);
        dataModel.put("employees", employees);
        dataModel.put("shiftPlan", shiftPlan);
        dataModel.put("calendar", calendar);
        dataModel.put("homeOfficeRecords", records);

        TemplateProcessor processor = TemplateProcessor.INSTANCE;
        processor.initConfiguration();
        StringWriter output = processor.processDocumentTemplate(dataModel, "shiftplan.ftl");

        DocGenerator docGenerator = new DocGenerator();
        Document document = docGenerator.getRawHTML(output.toString());
        docGenerator.createPDF(document, Path.of(System.getProperty("user.home"), "shiftplan.pdf"));

    }

    @Test
    void createCalendar() throws TemplateException, IOException, JDOMException {
        //TODO Testläufe bisher nur mit 6 Mitarbeitern in 3 Gruppen (entspricht der aktuellen Organisation der
        // Abteilung). Es sollten aber auch weitere Variationen getestet werden, z.B.: Änderung der Mitarbeiterzahl,
        // Änderung der Gruppenanzahl, der Länge der Homeoffice-Phasen
        DocumentParser docParser = new DocumentParser();
        docParser.parseDocument();

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

        TemplateProcessor processor = TemplateProcessor.INSTANCE;
        processor.initConfiguration();
        StringWriter output = processor.processDocumentTemplate(dataModel, "shiftplan.ftl");

        DocGenerator docGenerator = new DocGenerator();
        Document document = docGenerator.getRawHTML(output.toString());
        docGenerator.createPDF(document, Path.of(System.getProperty("user.home"), "shiftplan.pdf"));

        /*File outputFile = Path.of("/", "home", "stephan", "schichtplan.html").toFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8));

        writer.write(output.toString());
        writer.flush();*/
    }

    private Employee[] getEmployees() {

        Employee emp1a = new Employee("Hans", "Maier", Employee.PARTICIPATION_SCHEMA.HO_LS, "#0099ee");
        Employee emp1b = new Employee("Sabine", "Klein", Employee.PARTICIPATION_SCHEMA.HO_LS,"darkred");
        Employee emp2a = new Employee("Willi", "Schick",Employee.PARTICIPATION_SCHEMA.HO_LS,"yellowgreen");
        Employee emp2b = new Employee("Karla", "Meier",Employee.PARTICIPATION_SCHEMA.HO_LS,"orangered");
        Employee emp3a = new Employee("Otto", "Waalkes", Employee.PARTICIPATION_SCHEMA.HO_LS,"brown");
        Employee emp3b = new Employee("Natalie", "Schön", Employee.PARTICIPATION_SCHEMA.HO_LS,"darkgoldenrod");

        emp1a.addBackup(emp1b);
        emp1b.addBackup(emp1a);

        emp2a.addBackup(emp2b);
        emp2b.addBackup(emp2a);

        emp3a.addBackup(emp3b);
        emp3b.addBackup(emp3a);

        return new Employee[] {emp1a, emp1b, emp2a, emp2b, emp3a, emp3b};
    }

    private List<EmployeeGroup> createGroups() {

        // Group 1
        Employee emp1a = new Employee("Hans", "Maier", Employee.PARTICIPATION_SCHEMA.HO_LS, "#0099ee");
        Employee emp1b = new Employee("Sabine", "Klein", Employee.PARTICIPATION_SCHEMA.HO_LS,"darkred");
        // Group 2
        Employee emp2a = new Employee("Willi", "Schick",Employee.PARTICIPATION_SCHEMA.HO_LS,"yellowgreen");
        Employee emp2b = new Employee("Karla", "Meier",Employee.PARTICIPATION_SCHEMA.HO_LS,"orangered");
        // Group 3
        Employee emp3a = new Employee("Otto", "Waalkes", Employee.PARTICIPATION_SCHEMA.HO_LS,"brown");
        Employee emp3b = new Employee("Natalie", "Schön", Employee.PARTICIPATION_SCHEMA.HO_LS,"darkgoldenrod");

        EmployeeGroup group1 = new EmployeeGroup("Group 1", new Employee[] {emp1a, emp1b});
        EmployeeGroup group2 = new EmployeeGroup("Group 2", new Employee[] {emp2a, emp2b});
        EmployeeGroup group3 = new EmployeeGroup("Group 3", new Employee[] {emp3a, emp3b});

        return List.of(group1, group2, group3);
    }

    @Test
    void testDatesUntil() {
        LocalDate today = LocalDate.now();
        today.datesUntil(LocalDate.of(2023, 1,1))
                .forEach(nextDate -> logger.debug(nextDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))));
    }
}