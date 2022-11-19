package shiftplan.users;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Employee {

    private final String name;
    private final String lastName;
    private boolean lateShiftOnly = false;
    private final int lateShiftOrder; // -1 = Keine Spätschicht !!
    private final List<LocalDate> lateShiftPlan;
    private final List<LocalDate> holidays;
    private EmployeeGroup employeeGroup;

    public Employee(String aName, String aLastName, int shiftOrder) {
        name = aName;
        lastName = aLastName;
        lateShiftOrder = shiftOrder;
        lateShiftPlan = new ArrayList<>();
        holidays = new ArrayList<>();
    }

    public Employee(String name, String lastName, int shiftOrder, boolean lateShiftOnly) {
        this(name, lastName, shiftOrder);
        this.lateShiftOnly = lateShiftOnly;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isLateShiftOnly() {
        return lateShiftOnly;
    }

    public int getLateShiftOrder() {
        return lateShiftOrder;
    }

    void setEmployeeGroup(EmployeeGroup group) {
        employeeGroup = group;
    }

    public boolean isHomeOfficeDay(LocalDate date) {
        return employeeGroup.getHomeOfficePlan().contains(date);
    }

    public void addToLateShiftPlan(LocalDate date) {
        lateShiftPlan.add(date);
    }

    public List<LocalDate> getLateShiftPlan() {
        return lateShiftPlan;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Employee{");
        sb.append("name='").append(name).append('\'');
        sb.append(", lastName='").append(lastName).append('\'');
        sb.append(", lateShiftOrder='").append(lateShiftOrder).append('\'');
        sb.append(", lateShiftOnly=").append(lateShiftOnly);
        sb.append('}');
        return sb.toString();
    }
}
