package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.users.Employee;
import shiftplan.users.EmployeeGroup;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ShiftPlanner {

    private static final Logger logger = LogManager.getLogger(ShiftPlanner.class);
    private static final List<LocalDate> holidays;
    private final LocalDate startDate;
    private final LocalDate endDate;

    static {
        holidays = List.of(
                LocalDate.of(2022, 1, 1), // Neujahr
                LocalDate.of(2022, 4, 15), // Karfreitag
                LocalDate.of(2022, 4, 18), // Ostermontag
                LocalDate.of(2022, 6,6), // Pfingstmontag
                LocalDate.of(2022, 10, 3), // Tag der Einheit
                LocalDate.of(2022, 12,26) // 2. Weihnachtstag
        );
    }

    public ShiftPlanner() {
        // Plan für das gesamte, aktuelle Jahr
        int year = LocalDate.now().getYear();
        startDate = LocalDate.of(year,1,1);
        endDate = LocalDate.of(year +1, 1,1);
    }

    public ShiftPlanner(LocalDate startDate) {
        // Plan für ein Benutzer-definiertes Startdatum bis Ende desselben Jahres
        this.startDate = startDate;
        endDate = LocalDate.of(startDate.getYear() +1, 1,1);
    }

    public ShiftPlanner(LocalDate startDate, LocalDate endDate) {
        // Plan für ein Benutzer-definiertes Start- und Enddatum
        this.startDate = startDate;
        this.endDate = endDate.plusDays(1);
    }

    public Map<String, Shift> createShiftPlan(List<EmployeeGroup> employeeGroups) {
        Map<String, Shift> shiftPlan = new TreeMap<>();

        employeeGroups.forEach(employeeGroup -> {
            for (LocalDate date : employeeGroup.getHomeOfficePlan()) {
                Shift shift = shiftPlan.getOrDefault(date.toString(), new Shift(date));
                shift.setHomeOfficeGroup(employeeGroup);
                shiftPlan.putIfAbsent(date.toString(), shift);
            }
            for (Employee employee : employeeGroup.getEmployees()) {
                employee.getLateShiftPlan().forEach(date -> {
                    Shift shift = shiftPlan.getOrDefault(date.toString(), new Shift(date));
                    shift.setLateShift(employee);
                    shiftPlan.putIfAbsent(date.toString(), shift);
                });
            }
        });
        return shiftPlan;
    }

    public void createHomeOfficePlan(List<EmployeeGroup> employeeGroups, int homeOfficeDayCount) {
        assert employeeGroups != null && !employeeGroups.isEmpty();
        int forwardCount = employeeGroups.size() * homeOfficeDayCount;
        logger.debug("forwardCount = {}", forwardCount);
        int startIndex = 0;
        List<LocalDate> workDays = getWorkDays();
        logger.debug("workdays.size() (number of workdays) = {}", workDays.size());

        for (EmployeeGroup employeeGroup : employeeGroups) {
            for (int index = startIndex; index < workDays.size(); index +=forwardCount) {
                int outerBound = checkRangeInBounds(workDays.size(), index, homeOfficeDayCount);
                logger.trace("outerBound: {}", outerBound);
                if (outerBound > -1) {
                    List<LocalDate> dateRange = workDays.subList(index, outerBound);
                    employeeGroup.addToPlan(dateRange);
                }
            }
            startIndex += homeOfficeDayCount;
            logger.debug("startIndex (nach Ende der inneren (for-) Schleife: {}", startIndex);
        }
    }

    public void createLateShiftPlan(Employee[] employees, int shiftPeriod) {
        assert employees != null && employees.length > 0;

        int shiftDayCount = 1;
        int employeeIndex = 0;
        int updatedEmployeeIndex = 0;
        List<LocalDate> workDays = getWorkDays();

        for (LocalDate workday : workDays) {
            updatedEmployeeIndex = getNextEmployee(employees, workday, employeeIndex, shiftDayCount, shiftPeriod);
            logger.debug("updatedEmployeeIndex: {}", updatedEmployeeIndex);
            if (updatedEmployeeIndex != employeeIndex) {
                shiftDayCount = 1;
                employeeIndex = updatedEmployeeIndex;
            }
            Employee employee = employees[employeeIndex];
            logger.debug("Current employee: {}", employee.getName());
            employee.addToLateShiftPlan(workday);
            ++shiftDayCount;
            logger.debug("shiftDayCount (nach Hinzufügen einer Schicht für {}: {}", employee.getName(), shiftDayCount);
        }
    }

    private int getNextEmployee(Employee[] employees, LocalDate currentDate, int currentEmployeeIndex, int shiftDayCount, int shiftPeriod) {
        logger.debug("Current date: {} // currentEmployeeIndex: {} // shiftDayCount: {}",
                currentDate, currentEmployeeIndex, shiftDayCount);
        Employee employee = employees[currentEmployeeIndex];
        if (shiftDayCount >= shiftPeriod || (employee.isHomeOfficeDay(currentDate) && !employee.isLateShiftOnly())) {
            shiftDayCount = -1;
            ++currentEmployeeIndex;
            if (currentEmployeeIndex >= employees.length) {
                currentEmployeeIndex = 0;
            }
            return getNextEmployee(employees, currentDate, currentEmployeeIndex, shiftDayCount, shiftPeriod);
        }
        return currentEmployeeIndex;
    }

    private int checkRangeInBounds(int workDaysSize, int currentIndex, int homeOfficeDayCount) {
        for (int i = homeOfficeDayCount; i >= 1; --i) {
            int outerBound = currentIndex + i;
            if (outerBound <= workDaysSize) {
                return outerBound;
            }
        }
        return -1;
    }

    List<LocalDate> getWorkDays() {
        List<LocalDate> workDays = new ArrayList<>();
        startDate.datesUntil(endDate)
                .forEach(nextDate -> {
                    if (isWorkday(nextDate) && isNotHoliday(nextDate)) {
                        logger.trace("Nächstes Datum in Liste (Kein W/E, kein Feiertag: {}",
                                nextDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
                        workDays.add(nextDate);
                    }
                });
        return workDays;
    }

    boolean isWorkday(LocalDate date) {
        assert date != null;
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    boolean isNotHoliday(LocalDate date) {
        assert date != null;
        return !holidays.contains(date);
    }
}
