package shiftplan.calendar;

import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import shiftplan.data.DocumentParser;
import shiftplan.document.TemplateProcessor;
import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    void createLateShiftPlan() throws IOException, JDOMException {
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

        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(holidays, docParser.getYear(), startDate, endDate);
        shiftPlanner.createHomeOfficePlan(groups, homeOfficeDuration);

        for (EmployeeGroup group : groups) {
            logger.debug("HomeOffice-Termine für Gruppe {}: {}", group.getGroupName(), group.getHomeOfficePlan());
        }

        Employee[] employees = EmployeeGroup.getEmployeesInShiftOrder();
        logger.debug("Employees: {}", Arrays.stream(employees).toList());
        shiftPlanner.createLateShiftPlan(employees, lateShiftDuration);

        for (Employee employee : employees) {
            assertTrue(employee.getLateShiftPlan().size() > 0);
            logger.debug("Late shift of {}: {}", employee.getName(), employee.getLateShiftPlan());
        }
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
    void createCalendar() throws TemplateException, IOException {
        List<EmployeeGroup> groups = createGroups();
        ShiftPlanner shiftPlanner = ShiftPlanner.newInstance(new ArrayList<>(), 2023, null, null);

        shiftPlanner.createHomeOfficePlan(groups, 2);
        Employee[] employees = EmployeeGroup.getEmployeesInShiftOrder();
        shiftPlanner.createLateShiftPlan(employees,5);

        Map<String, Shift> shiftPlan = shiftPlanner.createShiftPlan(groups);
        LocalDate from = LocalDate.of(2022,1,1);
        LocalDate to = LocalDate.of(2022,12,31);

        ShiftCalendar shiftCalendar = new ShiftCalendar();
        LocalDate startOfFirstWeek = shiftCalendar.getFirstCalendarWeekStart(from.getYear());
        List<LocalDate[]> calendar = shiftCalendar.createCalendar(from.getYear(), startOfFirstWeek);

        List<Employee> allEmployees = EmployeeGroup.getAllEmployees();

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("startDate", from);
        dataModel.put("endDate", to);
        dataModel.put("employees", allEmployees);
        dataModel.put("shiftPlan", shiftPlan);
        dataModel.put("calendar", calendar);

        TemplateProcessor processor = TemplateProcessor.INSTANCE;
        StringWriter output = processor.processDocumentTemplate(dataModel, "shiftplan.ftl");

        File outputFile = Path.of("/", "home", "stephan", "schichtplan.html").toFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8));

        writer.write(output.toString());
        writer.flush();
    }

    private List<EmployeeGroup> createGroups() {

        // Group 1
        Employee emp1a = new Employee("Hans", "Maier",5, false, "#0099ee");
        Employee emp1b = new Employee("Sabine", "Klein",2, false,"darkred");
        // Group 2
        Employee emp2a = new Employee("Willi", "Schick",4, false,"yellowgreen");
        Employee emp2b = new Employee("Karla", "Meier",1, false,"orangered");
        // Group 3
        Employee emp3a = new Employee("Otto", "Waalkes",3, false,"brown");
        Employee emp3b = new Employee("Natalie", "Schön", 0, false,"darkgoldenrod");

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