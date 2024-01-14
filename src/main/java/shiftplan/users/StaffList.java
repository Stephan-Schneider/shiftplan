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
    private Path staffListDir;

    /**
     * Erstellt eine StaffList-Instanz ohne die Möglichkeit, die generierte Liste in eine Datei zu schreiben.
     * (Verwendung im Http-Modus)
     *
     * @param copy Kopie des Schichtplans.
     * @throws NullPointerException wenn <code>copy</code> null ist.
     *
     */
    public StaffList(ShiftPlanCopy copy) {
        this.shiftPlanCopy = Objects.requireNonNull(copy, "Keine Schichtplan-Kopie!");
    }

    /**
     * Erstellt eine StaffList-Instanz mit der Möglichkeit, die generierte Liste in eine Datei zu schreiben.
     * (Verwendung im SSH-Modus)
     *
     * @param copy Kopie des Schichtplans
     * @param pathToStaffListDir Verzeichnis, in welches die StaffList-Datei (zwischen-) gespeichert wird.
     * @throws NullPointerException wenn <code>copy</code> null ist.
     */
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
        if (startDate.getYear() > yearAsPerShiftplan) {
            // Der Schichtplan liegt in der Vergangenheit - die Änderung eines nicht mehr verwendeten, veralteten
            // Plans ist nicht sinnvoll
            throw new ShiftPlanSwapException("Es wird ein veralteter SchichtPlan verwendet!");
        }

        ShiftCalendar calendar = new ShiftCalendar(yearAsPerShiftplan);
        LocalDate firstCalendarWeekStartDate = calendar.getFirstCalendarWeekStart(yearAsPerShiftplan);
        if (startDate.getYear() < yearAsPerShiftplan) {
            // Die StaffList wird für einen Pan in einem nachfolgenden Jahr erstellt.
            // Im Normalfall werden StaffLists / Änderungen des Schichtplans für einen laufenden Schichtplan erstellt -
            // die Änderung eines existierenden, zukünftigen Plans ist jedoch auch legitim.
            // Die Erstellung der StaffList beginnt in diesem Fall am Anfang des künftigen Jahres.
            return new ShiftCalendar.CalendarInfo(1, firstCalendarWeekStartDate);
        }
        // Ein laufender Plan wird geändert. Erstellung der StaffList ausgehend vom aktuellen Datum.
        return calendar.getCalendarWeekOfDate(firstCalendarWeekStartDate, startDate, true);
    }

}
