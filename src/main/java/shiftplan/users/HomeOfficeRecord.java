package shiftplan.users;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.calendar.ShiftPolicy;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

public class HomeOfficeRecord implements Comparable<HomeOfficeRecord> {

    private static final Logger logger = LogManager.getLogger(HomeOfficeRecord.class);

    private static final int maxHoDayPerMonth = ShiftPolicy.INSTANCE.getMaxHoDaysPerMonth();

    private final int optionsInPlan;
    private final int notAssigned;
    private final int month;
    private final String employeeName;

    private static final List<HomeOfficeRecord> allRecords = new ArrayList<>();

    static private final Map<Integer, String> months;

    static {
        months = new HashMap<>();
        months.put(1, "Januar");
        months.put(2, "Februar");
        months.put(3, "März");
        months.put(4, "April");
        months.put(5, "Mai");
        months.put(6, "Juni");
        months.put(7, "Juli");
        months.put(8, "August");
        months.put(9, "September");
        months.put(10, "Oktober");
        months.put(11, "November");
        months.put(12, "Dezember");
    }

    public static void createHomeOfficeReport(Employee[] employees, LocalDate start, LocalDate end) {
        LocalDate endExclusive = end.plusDays(1);
        for (Employee employee : employees) {
            start.datesUntil(endExclusive, Period.ofMonths(1)).forEach(localDate -> {
                int month = localDate.getMonthValue();
                int hoDaysTotalPerMonth = employee.getHoDaysTotalPerMonth(month);
                logger.debug("MA {} in Monat {} zugewiesen: {}", employee.getName(), month, hoDaysTotalPerMonth);
                HomeOfficeRecord record = new HomeOfficeRecord(
                        employee.getName() + " " + employee.getLastName(),
                        month,
                        hoDaysTotalPerMonth,
                        maxHoDayPerMonth - hoDaysTotalPerMonth
                );
                addRecord(record);
            });
        }
    }

    private HomeOfficeRecord(String employeeName, int month, int optionsInPlan, int notAssigned) {
        this.employeeName = employeeName;
        this.month = month;
        this.optionsInPlan = optionsInPlan;
        this.notAssigned = notAssigned;
    }

    public int getOptionsInPlan() {
        return optionsInPlan;
    }

    public int getNotAssigned() {
        return notAssigned;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public int getMonth() {
        return month;
    }

    public String getMonthName() {
        return months.get(getMonth());
    }

    public static String valueOf(int monthValue) {
        return Objects.requireNonNull(months.get(monthValue), "monthValue out of range: " + monthValue + "!");
    }

    public static void addRecord(HomeOfficeRecord record) {
        allRecords.add(record);
    }

    public static void addRecord(List<HomeOfficeRecord> records) {
        allRecords.addAll(records);
    }

    public static List<HomeOfficeRecord> getAllRecords() {
        Collections.sort(allRecords);
        return allRecords;
    }

    @Override
    public String toString() {
        return "HomeOfficeRecord{" +
                "optionsInPlan=" + optionsInPlan +
                ", notAssigned=" + notAssigned +
                ", month=" + HomeOfficeRecord.valueOf(month) +
                '}';
    }

    @Override
    public int compareTo(HomeOfficeRecord other) {
        return Integer.compare(this.month, other.getMonth());
    }
}
