package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;

import java.time.LocalDate;

public class Shift {

    private static final Logger logger = LogManager.getLogger(Shift.class);

    private final LocalDate date;
    private Employee lateShift;
    private int homeOfficeSlots;
    private int maxHoDaysPerMonth;
    private Employee[] employeesInHo;
    private int index = -1;
    private EmployeeGroup homeOfficeGroup;

    public Shift(LocalDate date) {
        this.date = date;
    }

    public Shift(LocalDate date, int hoSlots, int maxHoDayPerMonth) {
        this.date = date;
        employeesInHo = new Employee[hoSlots];
        homeOfficeSlots = hoSlots;
        this.maxHoDaysPerMonth = maxHoDayPerMonth;
    }

    public int getHomeOfficeSlots() {
        return homeOfficeSlots;
    }

    public boolean hasBackupConflictWith(Employee hoCandidate) {
        // Prüft, ob einem Backup des Kandidaten an diesem Tag bereits ein HO-Tag zugeteilt wurde - falls ja, kann der
        // Slot nicht an den Kandidaten vergeben werden
        assert hoCandidate != null;
        if (hoCandidate.getBackups().isEmpty()) return false;

        for (Employee employeeInHo : employeesInHo) {
            if (employeeInHo != null && hoCandidate.getBackups().contains(employeeInHo)) {
                logger.info("HO-Kandidat {} {} hat Backup-Konflikt mit {} {}",
                        hoCandidate.getName(), hoCandidate.getLastName(),
                        employeeInHo.getName(), employeeInHo.getLastName());
                return true;
            }
        }
        return false;
    }

    public boolean isMaxPerMonthOverrun(Employee hoCandidate) {
        // Prüft, ob dem Kandidaten bereits die maximale Anzahl an HO-Tagen pro Monat zugeteilt wurde
        assert hoCandidate != null;

        boolean isOverrun =  hoCandidate.getHoDaysTotalPerMonth(date.getMonthValue()) >= maxHoDaysPerMonth;
        if (isOverrun) {
            logger.info("An MA {} {} können keine weiteren HO-Tage vergeben werden, da bereits {} Tage im " +
                            "Monat {} zugeteilt wurden ",
                    hoCandidate.getName(),
                    hoCandidate.getLastName(),
                    hoCandidate.getHoDaysTotalPerMonth(date.getMonthValue()),
                    date.getMonthValue()
            );
        }
        return isOverrun;
    }

    public void addEmployeeInHo(Employee employee) {
        ++index;
        if (index < employeesInHo.length) {
            employeesInHo[index] = employee;
            employee.countDownHomeOfficeBalance();
            employee.addToHoDaysTotal(date.getMonthValue());
        }
    }

    public Employee[] getEmployeesInHo() {
        return employeesInHo;
    }

    public boolean hasSlots() {
        return index < employeesInHo.length -1;
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
