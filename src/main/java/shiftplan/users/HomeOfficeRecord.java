package shiftplan.users;

import java.util.*;

public class HomeOfficeRecord implements Comparable<HomeOfficeRecord> {

    private final int optionsInPlan;
    private final int notAssigned;
    private final int month;
    private final String groupName;

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

    public HomeOfficeRecord(String groupName, int month, int optionsInPlan, int notAssigned) {
        this.groupName = groupName;
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

    public String getGroupName() {
        return groupName;
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
