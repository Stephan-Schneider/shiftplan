package shiftplan.users;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

class EmployeeGroupTest {

    private static Logger logger = LogManager.getLogger(EmployeeGroupTest.class);

    @Test
    void getEmployeesInShiftOrder(){
        List<EmployeeGroup> employeeGroups = createGroups();
        Employee[] employees = EmployeeGroup.getEmployeesInShiftOrder();
        for (Employee employee : employees) {
            logger.debug(employee.getName() + " / " + employee.getLateShiftOrder());
        }
    }

    private List<EmployeeGroup> createGroups() {

        // Group 1
        Employee emp1a = new Employee("Hans", "Maier",-5, false, "#0099ee");
        Employee emp1b = new Employee("Sabine", "Klein",2, false,"darkred");
        // Group 2
        Employee emp2a = new Employee("Willi", "Schick",-4, false,"yellowgreen");
        Employee emp2b = new Employee("Karla", "Meier",1, false,"orangered");
        // Group 3
        Employee emp3a = new Employee("Otto", "Waalkes",3, false,"brown");
        Employee emp3b = new Employee("Natalie", "Schön", -1, false,"darkgoldenrod");

        EmployeeGroup group1 = new EmployeeGroup("Group 1", new Employee[] {emp1a, emp1b});
        EmployeeGroup group2 = new EmployeeGroup("Group 2", new Employee[] {emp2a, emp2b});
        EmployeeGroup group3 = new EmployeeGroup("Group 3", new Employee[] {emp3a, emp3b});

        return List.of(group1, group2, group3);
    }
}