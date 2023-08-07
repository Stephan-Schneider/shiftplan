package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.ShiftPlanRunnerException;
import shiftplan.users.Employee;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class ShiftPlanner {

    private static final Logger logger = LogManager.getLogger(ShiftPlanner.class);
    private static List<LocalDate> holidays;
    private final LocalDate startDate;
    private final LocalDate endDate;


    public static ShiftPlanner newInstance(
            List<LocalDate> holidayList, int year, LocalDate startDate, LocalDate endDate)
            throws ShiftPlanRunnerException {
        assert holidayList != null;
        if (year < LocalDate.now().getYear()) {
            throw new ShiftPlanRunnerException(
                    "Ungültiges Jahr: " + year +
                            ". Schichtpläne können nur für das aktuelle oder kommende Jahre erstellt werden");
        }
        holidays = holidayList;

        logger.info("ShiftPlanner-Instanz wird erstellt");

        ShiftPlanner shiftPlanner;
        if (startDate == null) {
            shiftPlanner = new ShiftPlanner(year);
        } else if (endDate != null) {
            if (startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }
            shiftPlanner = new ShiftPlanner(startDate, endDate);
        } else {
            shiftPlanner = new ShiftPlanner(startDate);
        }
        return shiftPlanner;
    }

    private ShiftPlanner(int year) {
        // Plan für das gesamte in 'shiftplan.xml angegebene Jahr
        startDate = LocalDate.of(year,1,1);
        endDate = LocalDate.of(year +1, 1,1);
    }

    private ShiftPlanner(LocalDate startDate) {
        // Plan für ein Benutzer-definiertes Startdatum bis Ende desselben Jahres
        this.startDate = startDate;
        endDate = LocalDate.of(startDate.getYear() +1, 1,1);
    }

    private ShiftPlanner(LocalDate startDate, LocalDate endDate) {
        // Plan für ein Benutzer-definiertes Start- und Enddatum
        this.startDate = startDate;
        this.endDate = endDate.plusDays(1);
    }

    public Map<String, Shift> createLateShiftPlan(Employee[] employees) {
        assert employees != null && employees.length > 0;

        logger.info("Spätschichtplan wird erstellt");

        // Angestellte ausfiltern, die nicht an den Spätschichten teilnehmen und ausschließlich Homeoffice machen
        Employee[] lsCandidates = Arrays
                .stream(employees)
                .filter(employee -> employee.getParticipationSchema() != Employee.PARTICIPATION_SCHEMA.HO)
                .toArray(Employee[]::new);

        // Der Schlüssel der <lateShifts>-Map (<String> - Typ-Parameter) besteht aus einem java.time.LocaleDate.toString().
        // Die Konvertierung zu String ist notwendig, da sonst die LocaleDate-Keys der <calendarWeeks>-Map
        // im FTL-Template nicht auf die Schlüssel der (late)Shift-Map passen. Durch den String-Typ der Schlüssel
        // in lateShift werden die Schlüssel der <calendarWeeks> implizit in String konvertiert
        Map<String, Shift> lateShifts = new LinkedHashMap<>(); // Einfüge-Reihenfolge beim Iterieren der Map beibehalten

        ShiftPolicy policy = ShiftPolicy.INSTANCE;
        int shiftPeriod = policy.getLateShiftPeriod();
        //TODO: maxHOSlots und maxHoDaysPerMonth - Variablen löschen
        //int maxHOSlots = policy.getMaxHoSlots();
        //int maxHoDaysPerMonth = policy.getMaxHoDaysPerMonth();
        List<DayOfWeek> noLateShift = policy.getNoLateShiftOn();

        List<LocalDate> workdays = getWorkDays();
        // Zähler für die Anzahl der zugewiesenen Spätschichten. Das Maximum ist shiftDayCounter >= shiftPeriod
        int shiftDayCounter = 0;
        int employeeIndex = 0; // Index für den Zugriff auf die Employee's im lsCandidates-Array

        for (LocalDate workDay : workdays) {
            if (noLateShift.contains(workDay.getDayOfWeek())) {
                // Es ist zulässig, Wochentage festzulegen, an denen keine Spätschicht stattfinden soll
                // (z.B. montags). In diesem Fall wird ein Shift-Objekt ohne <lateShift> - Property erstellt und
                // der lateShifts-Map hinzugefügt.
                Shift emptyLateShift = new Shift(workDay);
                lateShifts.put(workDay.toString(), emptyLateShift);
                continue;
            }
            Shift lateShift = new Shift(workDay);
            if (shiftDayCounter >= shiftPeriod) {
                shiftDayCounter = 0;
                ++employeeIndex;
                if (employeeIndex >= lsCandidates.length) {
                    employeeIndex = 0;
                }
            }
            lateShift.setLateShift(lsCandidates[employeeIndex]);
            lateShifts.put(workDay.toString(), lateShift); // Konvertierung zu String
            ++shiftDayCounter;
        }
        return lateShifts;
    }

    public void createHomeOfficePlan(
            Employee[] employees, Map<String, Shift> shifts, Map<Integer, LocalDate[]> calendarWeeks) {

        logger.info("Homeoffice-Plan wird erstellt");

        int hoCreditsPerEmployee = ShiftPolicy.INSTANCE.getWeeklyHoCreditsPerEmployee();

        for (Map.Entry<Integer, LocalDate[]> entries : calendarWeeks.entrySet()) { // Schichtplan wochenweise durchlaufen
            Integer cwIndex = entries.getKey();
            LocalDate[] cw = entries.getValue();

            // Am Beginn einer neuen Kalenderwoche:
            // - Nicht eingeteilte HO-Credits dem NotAssignedHoDays-Konto jedes MA's hinzufügen
            // - Jedem MA die festgelegte Anzahl von HO-Credits neu zuweisen
            logger.debug("Auflistung nicht zugeteilter HO-Tage in KW {} je Mitarbeiter", cwIndex);
            for (Employee employee : employees) {
                employee.swapBalance();
                employee.setHomeOfficeBalance(hoCreditsPerEmployee);
            }

            // alle MA's, die in der laufenden Woche für eine Spätschicht eingeteilt sind (normalerweise maximal 2)
            List<Employee> lateShiftsInCurrentCW = lateShiftOfCurrentCW(cw, shifts, cwIndex);

            // Die MA's, die in der laufenden Woche in Spätschichten eingeteilt sind, aus der Angestelltenliste
            // herausfiltern. Sie werden zuletzt mit Top-Priorität auf den Employee-Stack gelegt
            List<Employee> employeesWithoutLateShift = new ArrayList<>(Arrays
                    .stream(employees)
                    .filter(employee -> !lateShiftsInCurrentCW.contains(employee))
                    .toList());

            // MA's priorisieren: diejenigen mit der höchsten Anzahl an nicht zugewiesenen HO-Tagen werden zuerst
            // berücksichtigt
            Collections.sort(employeesWithoutLateShift);
            Deque<Employee> employeeStack = new ArrayDeque<>();
            employeesWithoutLateShift.forEach(employeeStack::push);
            lateShiftsInCurrentCW.forEach(employeeStack::push); // Spätschichtmitarbeiter erhalten die höchste Priorität

            logger.debug("Prio-Liste in KW  {}:", cwIndex);
            AtomicInteger prioIndex = new AtomicInteger();
            employeeStack.iterator().forEachRemaining(employee -> logger.debug("{}. {}", prioIndex.incrementAndGet(), employee.getName()));

            assert employeeStack.size() == employees.length;

            for (LocalDate date : cw) { // Jeden Tag einer Kalenderwoche durchlaufen
                Shift shift = shifts.get(date.toString()); // <date> zu String konvertieren!!
                if (shift == null) continue; // bei <date> handelt es sich um einen Tag am Wochenende oder einen Feiertag

                // Liste der in <fillHoSlot> vom Employee-Stapel entfernten MA's, die grundsätzlich für HO-Tage qualifiziert
                // sind, aber am aktuellen Datum nicht eingeteilt werden können (Spätschicht, Vertretung) oder die
                // am aktuellen Datum eingeteilt wurden, aber für einen weiteren HO-Tag in der laufenden KW berechtigt
                // sind und für das folgende Datum wieder auf den Stapel gelegt werden müssen
                List<Employee> tmpList = new ArrayList<>();

                IntStream.rangeClosed(1, shift.getHomeOfficeSlots()).forEach(index -> {
                    // Die Methode wird so lange aufgerufen, bis die vorgegebene Anzahl an HO-Slots am laufenden Datum
                    // vergeben wurde
                    fillHoSlot(employeeStack, tmpList, shift);
                });
                // In <tmpList> befinden sich die höchst-priorisierten MA's am Anfang - auf den Stapel müssen sie aber
                // als letzte gelegt werden, um die ursprüngliche Priorisierung zu behalten. Daher wird die Reihenfolge
                // umgedreht
                Collections.reverse(tmpList);
                tmpList.forEach(employeeStack::push);
            }
        }
    }

    private void fillHoSlot(Deque<Employee> employeeStack, List<Employee> tmpList, Shift shift) {
        if (employeeStack.isEmpty()) return;
        if (!shift.hasSlots()) return;

        Employee lateShift = shift.getLateShift();
        Employee top = employeeStack.pop();
        logger.debug("Employee {} vom Stapel entfernt", top.getName());

        // fillHoSlots solange rekursiv aufrufen, bis ein HO-Slot vergeben wurde
        // MA's die grundsätzlich für HO qualifiziert sind, aber am aktuellen Datum kein HO wahrnehmen können
        // (Spätschicht, Vertretung) oder für einen weiteren HO-Tag in der laufenden Woche berechtigt sind,
        // werden in <tmpList> gespeichert
        if (top.equals(lateShift)) {
            if (!tmpList.contains(top)) {
                tmpList.add(top);
            }
            fillHoSlot(employeeStack, tmpList, shift);
        } else if (top.getParticipationSchema() == Employee.PARTICIPATION_SCHEMA.LS) {
            fillHoSlot(employeeStack, tmpList, shift);
        } else if (!top.hasDaysLeft()) {
            fillHoSlot(employeeStack, tmpList, shift);
        } else if (!top.equals(lateShift) && top.hasDaysLeft()) {
            if (Shift.hasBackupConflictWith(top, shift.getEmployeesInHo())) {
                if (!tmpList.contains(top)) tmpList.add(top);
                fillHoSlot(employeeStack, tmpList, shift);
            } else if (shift.isMaxPerMonthOverrun(top)) {
                fillHoSlot(employeeStack, tmpList, shift);
            } else {
                shift.addEmployeeInHo(top);
                logger.info("MA {} zum HO am {} eingeteilt", top.getName(), shift.getDate());
                if (top.hasDaysLeft() && !tmpList.contains(top)) tmpList.add(top);
            }
        }
    }

    private List<Employee> lateShiftOfCurrentCW(LocalDate[] cw, Map<String, Shift> shifts, int cwIndex) {
        Set<Employee> lateShiftOfCurrentWeek = new LinkedHashSet<>();
        for (LocalDate date : cw) {
            Shift shift = shifts.get(date.toString()); // Konvertierung von <date> zu String!!
            // Nicht allen Tage einer KW sind Schichten zugeordnet (z.B. Wochenende, Feiertag) und nicht jede Schicht
            // muss zwingend mit einer Spätschicht besetzt sein (z.B. Montags keine Spätschicht)
            if (shift != null && shift.getLateShift() != null) {
                // Einfaches Ausfiltern von Mehrfach-Einträgen eines Employees: Set erlaubt keine Duplikate
                lateShiftOfCurrentWeek.add(shift.getLateShift());
            }
        }

        List<Employee> tmpList = new ArrayList<>(lateShiftOfCurrentWeek);
        tmpList.forEach(employee -> logger.info("Spätschichten für KW {}: {}",
                cwIndex, employee));
        return tmpList;
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
