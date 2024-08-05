package shiftplan.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.calendar.ShiftCalendar;
import shiftplan.calendar.ShiftPlanCopy;
import shiftplan.calendar.ShiftPlanSwapException;
import shiftplan.calendar.ShiftPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;

public class StaffList {

    public record StaffData(String id, String displayName, TreeSet<Integer> cwIndices) {}

    private static final Logger logger = LogManager.getLogger(StaffList.class);

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

        // Mapping von Employee-Id und Spätschichttage-Zähler, mit dessen Hilfe nur die Startwoche einer Spätschicht
        // in die Stafflist eingetragen wird (bei Wochen-übergreifenden Spätschichten wird die zweite Kalenderwoche nicht
        // in die Stafflist eingetragen)
        Map<String, Integer> idIndexMap = new HashMap<>();
        int lateshiftPeriod = ShiftPolicy.INSTANCE.getLateShiftPeriod();


        for (Map.Entry<ShiftPlanCopy.CalendarWeek, ShiftPlanCopy.WorkDay[]> entry : shiftPlanCopy.getCalendarWeeks().entrySet()) {
            ShiftPlanCopy.CalendarWeek cw = entry.getKey();
            ShiftPlanCopy.WorkDay[] workDays = entry.getValue();
            int cwIndex = cw.cwIndex();

            if (cwIndex < info.calendarWeekIndex()) {
                // Die Kalenderwoche <cwIndex> liegt vor dem aktuellen Datum
                continue;
            }

            for (ShiftPlanCopy.WorkDay workDay : workDays) {
                Employee lateshift = workDay.getLateshift();
                if (lateshift != null) {
                    String id = lateshift.getId();
                    idIndexMap.putIfAbsent(id, 0);
                    if (idIndexMap.get(id) == lateshiftPeriod) {
                        // Die Spätschichtperiode des Mitarbeiters ist abgelaufen. Der Zähler wird zurückgesetzt
                        idIndexMap.put(id, 0);
                    }
                    // Den Spätschichtzähler des MA's <id> hochzählen
                    idIndexMap.merge(id, 1, Integer::sum);

                    String displayName = lateshift.getName() + " " + lateshift.getLastName();
                    StaffData staffData = employeeMap.get(id);
                    if (staffData == null) {
                        staffData = new StaffData(id, displayName, new TreeSet<>(Integer::compareTo));
                        employeeMap.put(id, staffData);
                    }
                    if (idIndexMap.get(id) == 1) {
                        // Nur bei der ersten registrierten Spätschicht innerhalb einer Spätschichtperiode wird der
                        // Kalenderwochenindex vermerkt. Solange die Spätschichtperiode eines MA's nicht abgelaufen ist,
                        // wird keine weitere Kalenderwoche eingetragen (und damit auch keine weitere KW, in welche die
                        // aktuelle Spätschicht eventuell fällt)
                        staffData.cwIndices().add(cwIndex);
                    }
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
        // Mitarbeiter-Liste auf Console ausdrucken - die Methode <printStaffList> wird nur bei Ausführung im
        // lokalen Modus aufgerufen
        String staffList = builder.toString();
        String separator = "\n\n***********************************************\n\n";
        System.out.println(separator + staffList + separator);
        return staffList;
    }

    public String serializeStaffList(Map<String, StaffData> employeeMap) {
        List<StaffData> staffDataList = employeeMap.values().stream().toList();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(staffDataList);
        } catch (JsonProcessingException e) {
            logger.error("StaffList kann nicht serialisiert werden", e);
            throw new ShiftPlanSwapException(e.getMessage());
        }
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
