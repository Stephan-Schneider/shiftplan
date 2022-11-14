package shiftplan.users;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmployeeGroup {

    private static final List<Employee> allEmployees = new ArrayList<>();

    private final String groupName;
    private final Employee[] employees;
    private final List<LocalDate> homeOfficePlan;

    public EmployeeGroup(String groupName, Employee[] employees) {
        this.groupName = groupName;
        this.employees = employees;
        allEmployees.addAll(Arrays.asList(employees));
        homeOfficePlan = new ArrayList<>();
    }

    public String getGroupName() {
        return groupName;
    }

    public List<LocalDate> getHomeOfficePlan() {
        return homeOfficePlan;
    }

    public void addToPlan(LocalDate date) {
        homeOfficePlan.add(date);
    }

    public void addToPlan(List<LocalDate> dates) {
        homeOfficePlan.addAll(dates);
    }

}
