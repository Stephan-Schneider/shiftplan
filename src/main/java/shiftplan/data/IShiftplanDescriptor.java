package shiftplan.data;

import org.apache.logging.log4j.Logger;
import shiftplan.users.Employee;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IShiftplanDescriptor {

    int getYear();

    LocalDate getStartDate();

    LocalDate getEndDate();

    List<LocalDate> getHolidays();

    Employee[] getEmployees();

    static void addBackups(
            List<Employee> employeeList,
            Map<Employee, List<String>> tmpEmployeeMap,
            Logger logger
    ) {
        for (Employee key : tmpEmployeeMap.keySet()) {
            List<String> ids = tmpEmployeeMap.get(key);
            List<Employee> backupsForKey = employeeList.stream().filter(
                    employee -> ids.contains(employee.getId())).toList();
            key.addBackups(backupsForKey);
            logger.info("MA {} {} mit diesen Backups erstellt: {}",
                    key.getName(), key.getLastName(), key.getBackups());
        }
    }
}
