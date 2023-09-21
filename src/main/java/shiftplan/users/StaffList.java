package shiftplan.users;

import shiftplan.calendar.ShiftCalendar;
import shiftplan.calendar.ShiftPlanCopy;
import shiftplan.calendar.ShiftPlanSwapException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;

public class StaffList {

    public record StaffData(String id, String displayName, TreeSet<Integer> cwIndices) {}

    private final ShiftPlanCopy shiftPlanCopy;
    private final Path staffListDir;

    public StaffList(ShiftPlanCopy copy, String pathToStaffListDir) {
        this.shiftPlanCopy = Objects.requireNonNull(copy, "Keine Schichtplan-Kopie!");
        Objects.requireNonNull(pathToStaffListDir, "Kein PFad zur Mitarbeiter-Liste");
        staffListDir = Path.of(pathToStaffListDir);
    }

    public Map<String, StaffData> createStaffList() {
        Map<String, StaffData> employeeMap = new HashMap<>();
        ShiftCalendar.CalendarInfo info = getCurrentCalendarWeek();

        for (Map.Entry<ShiftPlanCopy.CalendarWeek, ShiftPlanCopy.WorkDay[]> entry : shiftPlanCopy.getCalendarWeeks().entrySet()) {
            ShiftPlanCopy.CalendarWeek cw = entry.getKey();
            ShiftPlanCopy.WorkDay[] workDays = entry.getValue();
            int cwIndex = cw.cwIndex();

            if (cwIndex < info.calendarWeekIndex()) {
                continue;
            }

            for (ShiftPlanCopy.WorkDay workDay : workDays) {
                Employee lateshift = workDay.getLateshift();
                if (lateshift != null) {
                    String id = lateshift.getId();
                    String displayName = lateshift.getName() + " " + lateshift.getLastName();
                    StaffData staffData = employeeMap.get(id);
                    if (staffData == null) {
                        staffData = new StaffData(id, displayName, new TreeSet<>(Integer::compareTo));
                        employeeMap.put(id, staffData);
                    }
                    staffData.cwIndices().add(cwIndex);
                }
            }
        }
        return employeeMap;
    }

    public String printStaffList(Map<String, StaffData> employeeMap) {
        StringBuilder builder = new StringBuilder();
        for (String id : employeeMap.keySet()) {
            StaffData staffData = employeeMap.get(id);
            builder.append(id).append(" ").append(staffData.displayName).append(" ");
            StringJoiner sj = new StringJoiner(",", "{", "}");
            staffData.cwIndices.forEach(index -> sj.add(index.toString()));
            builder.append(sj).append("\n");
        }
        return builder.toString();
    }

    public void writeToFile(String staffData) throws IOException {
        if (!Files.isWritable(staffListDir)) {
            throw new ShiftPlanSwapException("Ungültiger Dateipfad für 'staff_list.txt");
        }
        Path staffListFile = staffListDir.resolve("staff_list.txt");
        Files.writeString(staffListFile, staffData,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    ShiftCalendar.CalendarInfo getCurrentCalendarWeek() {
        LocalDate startDate = LocalDate.now();
        int yearAsPerShiftplan = shiftPlanCopy.getForYear();
        if (startDate.getYear() != yearAsPerShiftplan) {
            throw new ShiftPlanSwapException("Es wird ein ungültiger SchichtPlan verwendet!");
        }
        ShiftCalendar calendar = new ShiftCalendar(yearAsPerShiftplan);
        LocalDate firstCalendarWeekStartDate = calendar.getFirstCalendarWeekStart(yearAsPerShiftplan);
        return calendar.getCalendarWeekOfDate(firstCalendarWeekStartDate, startDate, true);
    }

}
