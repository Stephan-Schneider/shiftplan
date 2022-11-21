package shiftplan.calendar;

import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;

import java.time.LocalDate;

public class Shift {

    private final LocalDate date;
    private Employee lateShift;
    private EmployeeGroup homeOfficeGroup;

    public Shift(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public Employee getLateShift() {
        return lateShift;
    }

    public void setLateShift(Employee lateShift) {
        this.lateShift = lateShift;
    }

    public EmployeeGroup getHomeOfficeGroup() {
        return homeOfficeGroup;
    }

    public void setHomeOfficeGroup(EmployeeGroup homeOfficeGroup) {
        this.homeOfficeGroup = homeOfficeGroup;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Shift{");
        sb.append("date=").append(date);
        sb.append(", lateShift=").append(lateShift);
        sb.append(", homeOfficeGroup=").append(homeOfficeGroup.getGroupName());
        sb.append('}');
        return sb.toString();
    }
}
