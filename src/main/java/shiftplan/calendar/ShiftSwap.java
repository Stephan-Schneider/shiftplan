package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.users.Employee;

import java.util.*;
import java.util.stream.IntStream;

public class ShiftSwap {

    private static final Logger logger = LogManager.getLogger(ShiftSwap.class);

    public static class SwapResult {

        private final OP_MODE swapMode;
        private final boolean swapHO;
        private final Employee employeeA;
        private final int undistributedHoA;
        private final Employee employeeB;
        private final int undistributedHoB;

        public SwapResult(OP_MODE swapMode, boolean swapHo,
                          Employee employeeA, int undistributedHoA,
                          Employee employeeB, int undistributedHoB) {
            this.swapMode = Objects.requireNonNull(swapMode);
            this.swapHO = swapHo;
            this.employeeA = Objects.requireNonNull(employeeA);
            this.undistributedHoA = undistributedHoA;
            this.employeeB = Objects.requireNonNull(employeeB);
            this.undistributedHoB = undistributedHoB;

        }

        public OP_MODE getSwapMode() {
            return swapMode;
        }

        public boolean isSwapHO() {
            return swapHO;
        }

        public Employee getEmployeeA() {
            return employeeA;
        }

        public int getUndistributedHoA() {
            return undistributedHoA;
        }

        public Employee getEmployeeB() {
            return employeeB;
        }

        public int getUndistributedHoB() {
            return undistributedHoB;
        }
    }

    private final ShiftPolicy policy = ShiftPolicy.INSTANCE;

    private final ShiftPlanCopy shiftPlanCopy;
    private final Map<ShiftPlanCopy.CalendarWeek, ShiftPlanCopy.WorkDay[]> calendarWeeks;
    private final Map<Integer, ShiftPlanCopy.WorkDay[]> simpleCalendarWeeks;
    private final List<ShiftPlanCopy.CalendarWeek> sortedKeys;
    private final int minIndex;
    private final int maxIndex;

    private final OP_MODE swapMode;
    private final boolean swapHO;


    /** Konstruktor mit den Default-Werten <code>SWAP_MODE.SWAP</code> und <code>swapHO == true</code>
     *
     * @param copy Kopie des Schichtplans, der auf Basis der XML-Serialisierung des Schichtplans rekonstruiert wird
     */
    public ShiftSwap(ShiftPlanCopy copy) {
        this(copy, OP_MODE.SWAP, true);
    }

    public ShiftSwap(ShiftPlanCopy copy, OP_MODE swap_mode, boolean swapHo) {
        shiftPlanCopy = copy;
        calendarWeeks = shiftPlanCopy.getCalendarWeeks();

        if (swap_mode != OP_MODE.SWAP && swap_mode != OP_MODE.REPLACE) {
            throw new ShiftPlanSwapException(
                    "Kein gültiger Modus! Es kann nicht festgestellt werden, " +
                            "ob Spätschichten getauscht oder eine Spätschicht ersetzt werden soll"
            );
        }
        this.swapMode = swap_mode;
        this.swapHO = swapHo;

        simpleCalendarWeeks = new HashMap<>();
        sortedKeys = new ArrayList<>(calendarWeeks.keySet());
        sortedKeys.sort(Comparator.comparing(ShiftPlanCopy.CalendarWeek::cwIndex));
        sortedKeys.forEach(calendarWeek -> simpleCalendarWeeks.put(calendarWeek.cwIndex(), calendarWeeks.get(calendarWeek)));

        minIndex = sortedKeys
                .stream()
                .map(ShiftPlanCopy.CalendarWeek::cwIndex)
                .mapToInt(Integer::intValue)
                .min()
                .orElse(-1);
        logger.debug("Kleinster Kalenderwochen-Index: {}", minIndex);


        maxIndex = sortedKeys
                .stream()
                .map(ShiftPlanCopy.CalendarWeek::cwIndex)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1);
        logger.debug("Größter KalenderWochen-Index: {}", maxIndex);
    }

    public List<ShiftPlanCopy.CalendarWeek> getCalendarWeeks() {
        return sortedKeys;
    }

    public Map<Integer, ShiftPlanCopy.WorkDay[]> getSimpleCalendarWeeks() {
        return simpleCalendarWeeks;
    }

    /**
     * Ändert den Schichtplan entweder in Form eines Spätschicht-Tauschs (SWAP_MODE.SWAP) oder indem
     * der Mitarbeiter mit ID <code>employee2ID</code> die Spätschicht vom Mitarbeiter mit ID <code>employee1Id</code>
     * übernimmt (SWAP_MODE.REPLACE) - ohne reziproken Tausch.
     * Optional können auch die durch die Änderung des Spätschichtplans eventuell entfallenden HomeOffice-Tage soweit
     * möglich verschoben werden.
     *
     * @param employee1Id ID des MA 1. In SWAP-Mode: replaced und replacer, in REPLACE_Mode: replaced
     * @param cwIndex1 Index der Kalenderwoche, in welcher der Spätschicht-Zyklus von MA 1 beginnt
     * @param employee2Id ID des MA 2. In SWAP-Mode: replaced und replacer, in REPLACE-Mode: replacer
     * @param optionalCwIndex2 Optionaler Index der Kalenderwoche, in welcher der Spätschicht-Zyklus von MA 2 beginnt.
     *                         Wird nur angegeben, wenn Spätschichten getauscht werden sollen (SWAP_MODE.SWAP)
     * @return SwapResult: ein Record der das Ergebnis der SchichtplanÄnderung festhält
     */
    public SwapResult swap(String employee1Id, int cwIndex1, String employee2Id, int... optionalCwIndex2) {
        assert optionalCwIndex2.length <= 1;
        int cwIndex2 = -1;

        if (swapMode == OP_MODE.SWAP) {
            if (optionalCwIndex2.length == 0) {
                throw new ShiftPlanSwapException(
                        "Für den Spätschicht-Tausch müssen die Kalenderwochen beider Mitarbeiter angegeben werden!");
            } else {
                cwIndex2 = optionalCwIndex2[0];
            }
        }

        Employee emp1 = shiftPlanCopy.getEmployeeById(employee1Id);
        Employee emp2 = shiftPlanCopy.getEmployeeById(employee2Id);
        if (employeeIsNull(emp1) || employeeIsNull(emp2)) {
            throw new ShiftPlanSwapException("Ungültige Mitarbeiter-ID's!");
        }

        if (swapMode == OP_MODE.SWAP) {
            if (indexOutOfRange(cwIndex1) || indexOutOfRange(cwIndex2)) {
                throw new ShiftPlanSwapException("Ungültige Kalenderwoche(n)!");
            }
            if (!isLateshift(emp1, cwIndex1) || !isLateshift(emp2, cwIndex2)) {
                throw new ShiftPlanSwapException("Keine Spätschicht des MA's in der angegebenen Spätschicht");
            }
        } else {
            if (indexOutOfRange(cwIndex1)) {
                throw new ShiftPlanSwapException("Ungültige Kalenderwoche!");
            }
            if (!isLateshift(emp1, cwIndex1)) {
                throw new ShiftPlanSwapException(("Ungültige Kalenderwoche"));
            }
        }

        if (swapMode == OP_MODE.REPLACE) {
            int cancelledHoDays = replaceLateShift(emp1, emp2, cwIndex1);
            if (swapHO) {
                // Der replacer (emp2) verliert durch die Übernahme der Spätschicht eventuell HO-Tage
                // Der replaced (emp1) kann eventuell die HO-Tage des replacers (emp2) übernehmen → freeSlots
                // Der Rückgabewert von swapHomeOfficeDays wird ignoriert, da er hier keinen Aussagewert hat
                swapHomeOfficeDays(emp1, emp2, cwIndex1, ShiftPolicy.INSTANCE.getWeeklyHoCreditsPerEmployee());
            }
            // Im REPLACE-Modus werden keine HO-Tage von replaced (emp1) storniert. Die Anzahl der stornierten
            // HO-Tage beträgt daher immer 0
            // Für replacer emp2 werden die stornierten HO-Tage festgehalten (es findet keine Neuzuteilung von
            // HO-Tagen für den replacer statt)
            return new SwapResult(swapMode, swapHO, emp1, 0, emp2, cancelledHoDays);
        } else if (swapMode == OP_MODE.SWAP) {
            int undistributedHoDaysEmp1 = 0;
            int undistributedHODaysEmp2 = 0;
            Map<String, Integer> cancelledHoDays = swapLateShift(emp1, cwIndex1, emp2, cwIndex2);
            if (swapHO) {
                undistributedHoDaysEmp1 = swapHomeOfficeDays(emp1, emp2, cwIndex1, cancelledHoDays.get(employee1Id));
                undistributedHODaysEmp2 = swapHomeOfficeDays(emp2, emp1, cwIndex2, cancelledHoDays.get(employee2Id));
            }
            return new SwapResult(swapMode, swapHO, emp1, undistributedHoDaysEmp1, emp2, undistributedHODaysEmp2);
        }
        return null;
    }

    int replaceLateShift(Employee replaced, Employee replacer, int cwIndex) {
        ShiftPlanCopy.WorkDay[] lateShifts = simpleCalendarWeeks.get(cwIndex);

        int lateShiftIndex = getIndexOfFirstLateShift(lateShifts, replaced);
        if (lateShiftIndex < 0) {
            throw new ShiftPlanSwapException(
                    "MA " + replaced.getName() + " hat keine Spätschicht in KW " + cwIndex + "!");
        }
        int lateShiftPeriod = policy.getLateShiftPeriod();
        int[] cancelledHoDays = new int[1];
        moveLateShift(lateShifts, cwIndex, lateShiftIndex, replaced, replacer, lateShiftPeriod, cancelledHoDays);
        return cancelledHoDays[0];
    }

     Map<String, Integer> swapLateShift(Employee employee1, int cwIndex1, Employee employee2, int cwIndex2) {
        ShiftPlanCopy.WorkDay[] emp1LateShifts = simpleCalendarWeeks.get(cwIndex1);
        ShiftPlanCopy.WorkDay[] emp2LateShifts = simpleCalendarWeeks.get(cwIndex2);

        int lateShiftIndexOfEmp1 = getIndexOfFirstLateShift(emp1LateShifts, employee1);
        if (lateShiftIndexOfEmp1 < 0) {
            throw new ShiftPlanSwapException("MA " + employee1.getName() +
                    " hat Keine Spätschicht in KW " + cwIndex1 + "!");
        }
        int lateShiftIndexOfEmp2 = getIndexOfFirstLateShift(emp2LateShifts, employee2);
        if (lateShiftIndexOfEmp2 < 0) {
            throw new ShiftPlanSwapException("MA " + employee2.getName() +
                    " hat Keine Spätschicht in KW " + cwIndex2 + "!");
        }

        int lateShiftPeriod = policy.getLateShiftPeriod();

        int[] cancelledHoEmp1 = new int[1];
        int[] cancelledHoEmp2 = new int[1];

        moveLateShift(emp1LateShifts, cwIndex1, lateShiftIndexOfEmp1, employee1, employee2, lateShiftPeriod, cancelledHoEmp2);
        moveLateShift(emp2LateShifts, cwIndex2, lateShiftIndexOfEmp2, employee2, employee1, lateShiftPeriod, cancelledHoEmp1);

        Map<String, Integer> cancelledHoDays = new HashMap<>();
        cancelledHoDays.put(employee1.getId(), cancelledHoEmp1[0]);
        cancelledHoDays.put(employee2.getId(), cancelledHoEmp2[0]);
        return cancelledHoDays;
    }

    /**
     * Austausch der Spätschichten zweier Mitarbeiter:
     *      Ausgangssituation:
     *      - Spätschicht von A in KW X
     *      - Spätschicht von B in KW Y
     *     Nach Tausch
     *     - Spätschicht A in KW Y
     *     - Spätschicht B in KW X
     * Der Austausch beginnt in den Kalenderwochen X bzw. Y. Je nach den jeweiligen Gegebenheiten (wochentag, auf den
     * der Beginn des Spätschichtzyklus fällt, Anzahl der Arbeitstage in der jeweiligen Kalenderwoche, Länge des
     * Spätschichtzyklus) kann sich ein Spätschichtzyklus über weitere Kalenderwochen erstrecken (X + 1..., Y +1...)
     * Die Methode wird nach Bearbeitung einer Kalenderwoche erneut rekursiv aufgerufen, bis alle Spätschichten des
     * Zyklus komplett neu zugewiesen wurden. Die Methode hält dabei die Anzahl der stornierten HomeOffice-Tage fest
     *
     *
     * @param week Kalenderwoche, in welcher der Spätschicht-Tausch beginnt.
     * @param cwIndex Index der Kalenderwoche, in welcher der Spätschichttausch beginnt.
     * @param startIndex Index des ersten Tages in <code>week</code>, auf welchen die erste Spätschicht des
     *                   Spätschicht-Zyklus von <code>replaced</code> fällt. Dieser Index wird hochgezählt, bis alle
     *                   Tage von <code>week</code> durchlaufen sind.
     * @param replaced Mitarbeiter, der / die die Spätschicht an <code>replacer</code> abgibt
     * @param replacer Mitarbeiter, dem / der die Spätschicht zugewiesen wird und damit <code>replaced</code> ersetzt
     * @param remainingShifts Anzahl der verbleibenden Spätschicht-Tage (bei jeder Zuteilung einer Spätschicht an
     *                        <code>replacer</code> wird die Anzahl um 1 reduziert)
     * @param cancelledHoDays Anzahl der stornierten HO-Tage von <code>replacer</code> - durch das Zuweisen von
     *                        Spätschichten, können HO-Tag von <code>replacer</code>  entfallen
     */
    void moveLateShift(
            ShiftPlanCopy.WorkDay[] week,
            int cwIndex,
            int startIndex,
            Employee replaced,
            Employee replacer,
            int remainingShifts,
            int[] cancelledHoDays
    ) {
        logger.info("Spätschichten von Kalenderwoche {} werden getauscht", cwIndex);
        logger.info("KW vor Tausch: {}", (Object) week);

        while (startIndex < week.length && remainingShifts > 0) {
            week[startIndex].setLateshift(replacer);
            cancelledHoDays[0] += week[startIndex].removeEmployeeInHo(replacer);
            ++startIndex;
            --remainingShifts;
        }

        logger.info("KW wird nach Änderung der Spätschicht mit Index {} im Kalender abgelegt", cwIndex);
        simpleCalendarWeeks.put(cwIndex, week);
        logger.info("KW nach Tausch: {}", (Object) week);
        if (remainingShifts > 0) {
            logger.info("Es sind noch nicht alle Schichten verteilt. <moveLateShift> wird erneut aufgerufen");
            ShiftPlanCopy.WorkDay[] nextWeek = getAdjacentCalendarWeek(cwIndex);
            if (nextWeek.length > 0) {
                int startIndexOfNextWeek = getIndexOfFirstLateShift(nextWeek, replaced);
                logger.trace("Nächste Spätschicht von replaced am weekArray-Index {}", startIndexOfNextWeek);
                if (startIndexOfNextWeek >= 0) {
                    moveLateShift(nextWeek, cwIndex +1, startIndexOfNextWeek, replaced, replacer,
                            remainingShifts, cancelledHoDays);
                }
            }
        }
    }

    /**
     * Beim Tauschen der Spätschichten verlieren die involvierten Angestellten u.U. (einen Teil) ihre HO-Tage, da
     * die Spätschicht ja auf eine Woche verlegt wird, in welcher der MA nach dem ursprünglichen Plan Normalschicht
     * hatte und daher für das Arbeiten im HO-Office eingeteilt war. Um die verlorenen HO-Tage zu kompensieren, werden
     * diese nach Möglichkeit in die Woche gelegt, in welcher der MA nach dem ursprünglichen Plan Spätschicht, aber
     * nach dem Spätschicht-Tausch, Normalschicht hat.
     *
     * @param hoCandidate Employee, dem oder der HO-Tage zugewiesen werden.
     * @param newLateshift Employee, der nach dem Wechsel der Spätschicht in <code>cwIndex</code> für die Spätschicht
     *                     eingeteilt ist. Anhand der Spätschicht prüft die Anwendung, ob nach dem Anfangsindex weitere,
     *                     nachfolgende Kalenderwochen zu berücksichtigen sind.
     *
     * @param cwIndex Index der Kalenderwoche, ab welcher die Verteilung der HomeOffice-Tage für <code>hoCandidate</code>
     *                beginnt. Je nachdem, wie viele Arbeitstage die Kalenderwoche hat und auf welchen Wochentag der
     *                Beginn dew Spätschichtzyklus fällt, kann sich der Spätschicht-Tausch auf eine, zwei oder sogar
     *                noch mehr Kalenderwochen erstrecken.
     * @param maxHoDaysToDistribute Maximale Anzahl der HO-Tage, die an den <code>hoCandidate</code> verteilt werden können.
     *                              Dabei handelt es sich bei einem Spätschichttausch (SWAP) um die Anzahl der beim Tausch
     *                              verloren (stornierten) HO-Tage.
     *                              Beim Ersetzen (REPLACE) ist dies die Anzahl der freien Slots
     * @return Differenz zwischen den maximal zu Verteilenden und den tatsächlich neu verteilten HO-Tagen
     */
    int swapHomeOfficeDays(Employee hoCandidate, Employee newLateshift, int cwIndex, int maxHoDaysToDistribute) {
        if (hoCandidate.getParticipationSchema() == Employee.PARTICIPATION_SCHEMA.LS) return 0;
        // Ein Wert < 0 signalisiert, daß in der aktuell untersuchten KW nicht mehr vom Spätschichttausch betroffen ist und
        // die Schleife / Methode abgebrochen werden kann.
        while (getIndexOfFirstLateShift(simpleCalendarWeeks.get(cwIndex), newLateshift) >= 0) {
            logger.info("HomeOffice-Tage in Kalenderwoche {} werden (sofern möglich) verteilt", cwIndex);
            ShiftPlanCopy.WorkDay[] week = simpleCalendarWeeks.get(cwIndex);
            int dayIndex = 0;
            // Anzahl der potenziell zu verteilenden HO-Tage. Dieser Wert hängt von der maximal erlaubten Anzahl von
            // HO-Tagen in einer Woche je Mitarbeiter und der Anzahl der noch vorhandenen, beim Spätschicht-Tausch nicht
            // stornierten HO-Tage ab (Bsp.: der MA hat an einem Montag HomeOffice, der Spätschicht-Zyklus beginnt an
            // einem Dienstag, der Montag bleibt also als HomeOffice-Tag erhalten)
            int remainingWeeklyHoCredits = openHoCreditsInWeek(week, hoCandidate);
            logger.info("{} HomeOffice-Tage können in KW {} maximal zugewiesen werden", remainingWeeklyHoCredits, cwIndex);
            while (dayIndex < week.length && maxHoDaysToDistribute > 0) {
                // Der Verteilungsalgorithmus für die HO-Tage befolgt folgende Regeln:
                //  - Max. <maxHODaysPerWeek> HO-Tage pro Woche
                //  - Max. <maxHOSlotsPerDay> MA's im Home-Office an einem Tag
                //  - Keinen Backup-Konflikt mit als Backup zugeteilten MA's
                //  - Home-Office schließt Spätschicht aus und umgekehrt
                // NICHT befolgt wird die <maxHODaysPerMonth> - Regel, da auf Basis der übergebenen Daten nicht geprüft
                // werden kann, ob die maximale Anzahl von Home-Office-Tagen pro Monat bei der Zuteilung von HO-Tagen
                // überschritten wird.
                if (remainingWeeklyHoCredits == 0) {
                    logger.info("Alle HomeOffice-Optionen in KW {} erschöpft. Wechsel zur nächsten KW ...", cwIndex);
                    // Keine HO-Tage mehr in der laufenden Woche zu verteilen, Wochenschleife (innere Schleife)
                    // abbrechen, um mit der nächsten Kalenderwoche fortzufahren
                    break;
                }
                ShiftPlanCopy.WorkDay workday = week[dayIndex];
                if (workday.hasHoDay(hoCandidate)) {
                    // Übriggebliebener, beim Spätschichttausch nicht stornierter HO-Tag. Es kann mit dem nächsten
                    // Tag der Woche fortgefahren werden
                    ++dayIndex;
                    continue;
                }
                if (workday.getFreeSlots() > 0 && !workday.hasBackupConflictWith(hoCandidate) &&
                        // In Ausnahmefällen (Wenn Spätschichten in benachbarten Kalenderwochen getauscht werden)
                        // kann die Einteilung ins Homeoffice mit der Spätschichtzuteilung (infolge des Tauschs)
                        // konfligieren
                        !hoCandidate.equals(workday.getLateshift())) {
                    logger.info("HomeOffice-Einteilung am {} ({})", workday.getDate(), workday.getDayOfWeek());
                    workday.addEmployeeInHo(hoCandidate);
                    --remainingWeeklyHoCredits;
                    --maxHoDaysToDistribute;
                }
                ++dayIndex;
            }
            if (maxHoDaysToDistribute == 0) {
                if (swapMode == OP_MODE.SWAP) {
                    logger.debug("Keine weiteren HO-Tage zu verteilen - Maximum erreicht (SWAP)");
                    // Die beim Spätschichttausch stornierten Homeoffice - Tage sind alle neu verteilt worden, die
                    // HO-Verteilung wird daher abgebrochen (nur bei SWAP-Mode)
                    return 0;
                }
            }
            if (swapMode == OP_MODE.REPLACE) {
                // Maximal zu verteilende HO-Tage für die nächste Kalenderwoche bestimmen
                logger.debug("Vor Wechsel in nächste KW Neuzuteilung der max. HO-Tage / MA pro Woche (REPLACE) ");
                maxHoDaysToDistribute = ShiftPolicy.INSTANCE.getWeeklyHoCreditsPerEmployee();
            }
            ++cwIndex;
        }
        return maxHoDaysToDistribute;
    }

    boolean indexOutOfRange(int index) {
        return index < minIndex || index > maxIndex;
    }

    boolean employeeIsNull(Employee employee) {
        return employee == null;
    }

    boolean isLateshift(Employee employee, int cwIndex) {
        if (employee.getParticipationSchema() == Employee.PARTICIPATION_SCHEMA.HO) return false;

        ShiftPlanCopy.WorkDay[] workDays = simpleCalendarWeeks.get(cwIndex);
        ShiftPlanCopy.WorkDay workDayWithMatchingLateShift = Arrays
                .stream(workDays)
                .filter(workDay -> employee.equals(workDay.getLateshift()))
                .findFirst()
                .orElse(null);
        return workDayWithMatchingLateShift != null;
    }

    int getIndexOfFirstLateShift(ShiftPlanCopy.WorkDay[] workDays, Employee employee) {
        if (workDays == null) return -1;
        return IntStream.range(0, workDays.length)
                .filter(index -> employee.equals(workDays[index].getLateshift()))
                .findFirst()
                .orElse(-1);
    }

    int openHoCreditsInWeek(ShiftPlanCopy.WorkDay[] week, Employee employee) {
        assert week != null;
        // Die Methode zählt zunächst die Anzahl der HomeOffice-Tage, die dem employee in der Kalenderwoche week
        // zugeteilt sind.
        // Anschließend ergibt sich die Anzahl der noch möglichen weiteren HomeOffice-Tage in der gegebenen Woche aus der
        // Differenz zwischen den maximal erlaubten HO-Tagen pro Woche und der Anzahl der bereits zugeteilten HO-Tage.
        int maxHoCreditsPerWeek = policy.getWeeklyHoCreditsPerEmployee();
        int hoDaysCounter = 0;
        for (ShiftPlanCopy.WorkDay workDay : week) {
            if (workDay.hasHoDay(employee)) {
                ++hoDaysCounter;
            }
        }
        return maxHoCreditsPerWeek - hoDaysCounter;
    }

    int getFreeHOSlotsInWeek(int cwIndex) {
        assert cwIndex <= maxIndex;
        ShiftPlanCopy.WorkDay[] week = simpleCalendarWeeks.get(cwIndex);
        return Arrays.stream(week).mapToInt(ShiftPlanCopy.WorkDay::getFreeSlots).reduce(0, Integer::sum);
    }

    ShiftPlanCopy.WorkDay[] getAdjacentCalendarWeek(int cwIndex) {
        return simpleCalendarWeeks.getOrDefault(cwIndex +1, new ShiftPlanCopy.WorkDay[0]);
    }
}
