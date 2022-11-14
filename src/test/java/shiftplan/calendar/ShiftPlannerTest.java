package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShiftPlannerTest {

    private static final Logger logger = LogManager.getLogger(ShiftPlannerTest.class);

    @Test
    void getWorkDays() {
        LocalDate startDate = LocalDate.of(2022, 1, 1);
        ShiftPlanner shiftPlanner = new ShiftPlanner(startDate);
        List<LocalDate> workdays = shiftPlanner.getWorkDays();
        LocalDate lastElement = workdays.get(workdays.size() -1);
        assertEquals(LocalDate.of(2022,12,30), lastElement);
        assertFalse(workdays.contains(LocalDate.of(2022,1,9))); // 09.01.2022 war Sonntag
        assertFalse(workdays.contains(LocalDate.of(2022,6,6))); // Pfingstmontag
    }

    @Test
    void createHomeOfficePlan() {
        List<EmployeeGroup> groups = createGroups();
        LocalDate startDate = LocalDate.of(2022, 1,1); // ab 01.01.2022
        ShiftPlanner shiftPlanner = new ShiftPlanner(startDate);
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

    private List<EmployeeGroup> createGroups() {

        // Group 1
        Employee emp1 = new Employee("Hans");
        // Group 2
        Employee emp2 = new Employee("Willi");
        // Group 3
        Employee emp3 = new Employee("Otto");

        EmployeeGroup group1 = new EmployeeGroup("Group 1", new Employee[] {emp1});
        EmployeeGroup group2 = new EmployeeGroup("Group 2", new Employee[] {emp2});
        EmployeeGroup group3 = new EmployeeGroup("Group 3", new Employee[] {emp3});

        return List.of(group1, group2, group3);
    }
}