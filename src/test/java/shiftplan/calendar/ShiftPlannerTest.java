package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShiftPlannerTest {

    private static final Logger logger = LogManager.getLogger(ShiftPlannerTest.class);

    @Test
    void getWorkDaysForCompleteCurrentYear() {
        ShiftPlanner shiftPlanner = new ShiftPlanner();
        List<LocalDate> workdays = shiftPlanner.getWorkDays();
        LocalDate lastElement = workdays.get(workdays.size() -1);

        assertEquals(LocalDate.of(2022,1,3), workdays.get(0));
        assertEquals(LocalDate.of(2022,12,30), lastElement);
        assertFalse(workdays.contains(LocalDate.of(2022,1,9))); // 09.01.2022 war Sonntag
        assertFalse(workdays.contains(LocalDate.of(2022,6,6))); // Pfingstmontag
    }

    @Test
    void getWorkDaysFromStartDateToEndOfYear() {
        ShiftPlanner shiftPlanner = new ShiftPlanner(LocalDate.now());
        List<LocalDate> workdays = shiftPlanner.getWorkDays();
        LocalDate lastElement = workdays.get(workdays.size() -1);

        assertEquals(LocalDate.now(), workdays.get(0));
        assertEquals(LocalDate.of(2022,12,30), lastElement);
        assertFalse(workdays.contains(LocalDate.of(2022,12,26)));
    }

    @Test
    void getWorkDaysFromStartToEnd() {
        LocalDate startDate = LocalDate.of(2022,4,1);
        LocalDate endDate = LocalDate.of(2022,9,30);
        ShiftPlanner shiftPlanner = new ShiftPlanner(startDate, endDate);
        List<LocalDate> workdays = shiftPlanner.getWorkDays();
        LocalDate lastElement = workdays.get(workdays.size() -1);

        assertEquals(LocalDate.of(2022,4,1), workdays.get(0));
        assertEquals(LocalDate.of(2022,9,30), lastElement);
        assertFalse(workdays.contains(LocalDate.of(2022,4,18)));

    }

    @Test
    void createHomeOfficePlan() {
        List<EmployeeGroup> groups = createGroups();
        //ShiftPlanner shiftPlanner = new ShiftPlanner();
        //LocalDate startDate = LocalDate.now();
        //ShiftPlanner shiftPlanner = new ShiftPlanner(startDate);
        LocalDate startDate = LocalDate.of(2022,4,1);
        LocalDate endDate = LocalDate.of(2022,9,30);
        ShiftPlanner shiftPlanner = new ShiftPlanner(startDate, endDate);
        shiftPlanner.createHomeOfficePlan(groups, 2);
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
    void createShiftPlan() {
        List<EmployeeGroup> groups = createGroups();
        ShiftPlanner shiftPlanner = new ShiftPlanner();
        shiftPlanner.createHomeOfficePlan(groups, 2);

        for (EmployeeGroup group : groups) {
            logger.debug("HomeOffice-Termine für Gruppe {}: {}", group.getGroupName(), group.getHomeOfficePlan());
        }

        Employee[] employees = EmployeeGroup.getEmployeesInShiftOrder();
        logger.debug("Employees: {}", Arrays.stream(employees).toList());
        shiftPlanner.createLateShiftPlan(employees, 5);

        for (Employee employee : employees) {
            assertTrue(employee.getLateShiftPlan().size() > 0);
            logger.debug("Late shift of {}: {}", employee.getName(), employee.getLateShiftPlan());
        }
    }

    private List<EmployeeGroup> createGroups() {

        // Group 1
        Employee emp1a = new Employee("Hans", "Maier",5);
        Employee emp1b = new Employee("Sabine", "Klein",2);
        // Group 2
        Employee emp2a = new Employee("Willi", "Schick",4);
        Employee emp2b = new Employee("Karla", "Meier",1);
        // Group 3
        Employee emp3a = new Employee("Otto", "Waalkes",3);
        Employee emp3b = new Employee("Natalie", "Schön", 0);

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