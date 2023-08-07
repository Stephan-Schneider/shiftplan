package shiftplan.calendar;

import shiftplan.users.Employee;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class ShiftPlanCopy {

    public record CalendarWeek(int cwIndex, LocalDate[] cwDates) {
        public  static LocalDate[] createCalendarWeekArray(LocalDate from, LocalDate to) {
            return from.datesUntil(to.plusDays(1)).toArray(LocalDate[]::new);
        }
    }

    private final int forYear;
    private final LocalDate from;
    private final LocalDate to;
    private final Employee[] employees;
    private final Map<String, Employee> employeeMap = new HashMap<>();
    private final Map<CalendarWeek, WorkDay[]> calendarWeeks = new TreeMap<>(Comparator.comparing(CalendarWeek::cwIndex));

    private final static ShiftPolicy policy = ShiftPolicy.INSTANCE;

    public ShiftPlanCopy(int forYear, LocalDate from, LocalDate to, Employee[] employees) {
        this.forYear = forYear;
        this.from = from;
        this.to = to;
        this.employees = employees;
        Arrays.asList(employees).forEach(employee -> employeeMap.put(employee.getId(), employee));
    }

    public int getForYear() {
        return forYear;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public Employee[] getEmployees() {
        return employees;
    }

    public Employee getEmployeeById(String id) {
        return employeeMap.get(id);
    }

    public Map<CalendarWeek, WorkDay[]> getCalendarWeeks() {
        return calendarWeeks;
    }

    public void addCalendarWeek(CalendarWeek calendarWeek, List<WorkDay> workDays) {
        WorkDay[] workdayArray = workDays.toArray(new WorkDay[0]);
        calendarWeeks.put(calendarWeek, workdayArray);
    }

    public static class WorkDay {

        private final DayOfWeek dayOfWeek;
        private final LocalDate date;
        private final boolean isLateShift;
        private Employee lateshift;
        private final List<Employee> employeesInHo;

        public WorkDay(DayOfWeek dayOfWeek, LocalDate date, boolean isLateShift) {
            this.dayOfWeek = dayOfWeek;
            this.date = date;
            this.isLateShift = isLateShift;
            employeesInHo = new ArrayList<>();
        }

        public DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }

        public LocalDate getDate() {
            return date;
        }

        public boolean isLateShift() {
            return isLateShift;
        }

        public Employee getLateshift() {
            return lateshift;
        }

        public void setLateshift(Employee lateshift) {
            this.lateshift = lateshift;
        }

        public List<Employee> getEmployeesInHo() {
            return employeesInHo;
        }

        public void addEmployeeInHo(Employee employee) {
           employeesInHo.add(employee);
        }

        public boolean hasHoDay(Employee employee) {
            return employeesInHo.contains(employee);
        }

        public int removeEmployeeInHo(Employee employee) {
            var ref = new Object() {
                int counter = 0;
            };
            employeesInHo.removeIf(emp-> {
                boolean isSameEmployee = emp.getId().equals(employee.getId());
                if (isSameEmployee) ++ref.counter;
                return isSameEmployee;
            });
            return ref.counter;
        }

        public int getFreeSlots() {
            return policy.getMaxHoSlots() - employeesInHo.size();
        }

        public boolean hasBackupConflictWith(Employee hoCandidate) {
            Employee[] employees = employeesInHo.toArray(new Employee[0]);
            return Shift.hasBackupConflictWith(hoCandidate, employees);
        }

        @Override
        public String toString() {
            return "WorkDay{" +
                    "dayOfWeek=" + dayOfWeek +
                    ", date=" + date +
                    ", isLateShift=" + isLateShift +
                    ", lateshift=" + lateshift +
                    ", employeesInHo=" + employeesInHo.size() +
                    '}';
        }
    }
}
