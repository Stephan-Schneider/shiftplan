package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import shiftplan.data.InvalidShiftPlanException;
import shiftplan.data.ShiftPlanSerializer;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BoundaryHandler {

    private static final Logger logger = LogManager.getLogger(BoundaryHandler.class);

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String shiftplanCopy;
    private final String shiftplanXSD;

    private boolean boundaryStrict = false;

    private final ShiftCalendar shiftCalendar;

    /**
     * Klasse zur Festlegung der Grenzen des Schichtplans (Beginn und Ende). Die Grenzen können in
     * zwei Modi festgelegt werden: strikt und nicht-strikt.
     * Im strikten Modus beginnt der Plan am ersten Werktag des Beginn-Monats und endet am letzten Werktag des
     * End-Monats.
     * Im nicht-strikten Modus beginnt der Plan an einem Montag, der dem 01. des Beginn-Monats am nächsten liegt
     * und endet an einem Freitag, der dem letzten Tag des End-Monats am nächsten liegt. Der Plan erstreckt
     * sich also immer über komplette Wochen.
     *
     * @param start Das Datum, an welchem der Schichtplan beginnt. Der Standardwert, den sich die
     *              Anwendung aus der Konfigurationsdatei (shiftplan.xml oder shiftplan.json) zieht, ist
     *              immer der 01. des betreffenden Startmonats - d.h. der Plan kann an jedem beliebigen
     *              Wochentag beginnen.
     * @param end   Das Datum, an welchem der Schichtplan endet. Das aus shiftplan.xml oder shiftplan.json
     *              geparste Datum ist immer der letzte Tag des jeweiligen Monats - d.h. der Plan kann
     *              an jedem beliebigen Wochentag enden.
     * @param shiftplanCopy Pfad zur Datei 'shiftplan_serialized.xml'
     * @param shiftplanXSD Pfad zur Datei 'shiftplan_serialized.xsd' (Schema zur Validierung)
     */
    public BoundaryHandler(LocalDate start, LocalDate end, String shiftplanCopy, String shiftplanXSD) {
        startDate = Objects.requireNonNull(start, "Fehlendes Startdatum");
        endDate = Objects.requireNonNull(end, "Fehlendes Enddatum");
        assert start.getYear() == end.getYear();

        this.shiftplanCopy = shiftplanCopy;
        this.shiftplanXSD = shiftplanXSD;

        shiftCalendar = new ShiftCalendar(startDate.getYear());
    }

    public void setBoundaryStrict(boolean strict) {
        this.boundaryStrict = strict;
    }

    public LocalDate getStartDate() {
        int shiftplanForYear = startDate.getYear();
        LocalDate firstCalendarWeekStart = shiftCalendar.getFirstCalendarWeekStart(shiftplanForYear);

        if (boundaryStrict) {
            if (startDate.getMonth() == Month.JANUARY) {
                // Die erste Kalenderwoche eines Jahres (KW 1) kann nach dem 01.01. des betreffenden Jahres beginnen.
                // In diesen Fällen liegt der Beginn des Schichtplans, der normalerweise mit dem 01. des Monats angegeben
                // wird, vor dem Beginn der ersten Kalenderwoche (Bsp.: 01.01. = Freitag). Das Startdatum des Kalenders
                // muss daher in diesem speziellen Fall am Jahresanfang auf das Datum des ersten Tages der KW 1 gesetzt
                // werden, um einen korrekten Ablauf des Programms zu gewährleisten.
                if (startDate.isBefore(firstCalendarWeekStart)) {
                    return firstCalendarWeekStart;
                }
            }
            return startDate;
        }
        LocalDate adjacentMonday = getAdjacentMonday();

        if (adjacentMonday.isBefore(firstCalendarWeekStart)) {
            // Das Zurückdatieren des Startdatums kann zur Folge haben, dass das Startdatum in die letzte Kalenderwoche
            // des Vorjahres fällt. Da die Ermittlung der Startkalenderwoche in ShiftCalender immer mit der ersten KW
            // beginnt, führt die Rückdatierung des Startdatums in die letzte KW des Vorjahres zu einer Fehlfunktion.
            // Das Startdatum muss daher auf den ersten Montag der 1. KW gesetzt werden.
            // Bsp.: der 01.01. ist ein Freitag (= KW 52/53), die KW 1 beginnt am 04.01. Der Montag wird zunächst auf
            // den 28.12, also in die letzte KW des Vorjahres zurückgesetzt und muss anschließend auf den 04.01.
            // korrigiert werden.
            return firstCalendarWeekStart;
        }
        ShiftPlanCopy copy = getShiftplanCopy();
        if (copy == null) {
            // Es existiert kein Schichtplan für eine vorherige Periode. Der Beginn des Schichtplans wird
            // daher auf den Montag in der gleichen Woche gesetzt, in die der 01. des Monats fällt, sofern
            // dieser auf einen Werktag fällt. Fällt der 01. des Monats auf das Wochenende, ist der Montag der
            // darauffolgenden Woche der Schichtplan-Start
            return adjacentMonday;
        }
        // Nach überlappender Kalenderwoche (Überlappung vorheriger und neuer Schichtplan) suchen.
        // Eine Überlappung besteht, wenn in der durch die Adjustierung des Plan-Beginns (adjacentMonday) festgelegten
        // Kalenderwoche ein übereinstimmendes Datum im vorherigen Plan existiert.
        List<LocalDate> week = adjacentMonday.datesUntil(adjacentMonday.plusDays(5)).toList();
        ShiftPlanCopy.WorkDay[] overlappingWeek = findOverlappingWeek(copy, week);
        if (overlappingWeek == null || overlappingWeek.length == 0) {
            // Es existiert keine überlappende Schichtwoche, daher wird der nächste Montag zurückgegeben
            // (siehe oben)
            return adjacentMonday;
        }

        LocalDate lastDay = overlappingWeek[overlappingWeek.length -1].getDate();
        return switch (lastDay.getDayOfWeek()) {
            // Endet der vorherige Schichtplan an einem Montag oder Dienstag, beginnt der neue Plan in der gleichen
            // kalenderwoche an einem Montag
            case MONDAY, TUESDAY -> adjacentMonday;
            // Endet der vorherige Schichtplan an einem Mittwoch, Donnerstag oder Freitag, wird der Beginn des neuen
            // Schichtplans auf den Montag der folgenden Woche gelegt.
            case WEDNESDAY -> lastDay.plusDays(5);
            case THURSDAY -> lastDay.plusDays(4);
            case FRIDAY -> lastDay.plusDays(3);
            default -> null;
        };
    }

    public LocalDate getEndDate() {
        if (boundaryStrict) {
            if (endDate.getMonth() == Month.DECEMBER) {
                // Erläuterungen über den Grund einer eventuellen Adjustierung des Enddatum im Dezember siehe Kommentar
                // in <adjustEndDateForDecember>
                return shiftCalendar.adjustEndDateForDecember(endDate);
            }
            return endDate;
        } else {
            // Prüfen, ob der adjustierte Freitag als Enddatum des Kalenders in KW 1 des Folgejahres liegt.
            // Falls ja, um 7 Tage zurücksetzen (= Freitag der letzten KW im Schichtplan-Jahr)
            LocalDate adjacentFriday = getAdjacentFriday();
            return shiftCalendar.adjustEndDateForDecemberFullWeek(adjacentFriday);
        }
    }

    private ShiftPlanCopy getShiftplanCopy() {
        ShiftPlanSerializer serializer = new ShiftPlanSerializer(shiftplanCopy, shiftplanXSD);
        try {
            return serializer.deserializeShiftplan();
        } catch (IllegalArgumentException ex) {
            logger.warn("Es existiert keine Schichtplan-Kopie oder XSD-Schema");
            return null;
        } catch (IOException | JDOMException ex) {
            throw new InvalidShiftPlanException(ex.getMessage());
        }
    }

    private LocalDate getAdjacentMonday() {
        DayOfWeek dayOfWeek = startDate.getDayOfWeek();

        // Fällt das Startdatum (01. des Monats) auf einen Werktag innerhalb der betroffenen Woche, wird das
        // Startdatum auf den Montag dieser Woche zurückdatiert. Fälls der Erste des Monats auf ein Wochenende, wird
        // auf den Montag der nächsten Woche vordatiert.
        return switch (dayOfWeek) {
            case MONDAY -> startDate;
            case TUESDAY -> startDate.minusDays(1);
            case WEDNESDAY -> startDate.minusDays(2);
            case THURSDAY -> startDate.minusDays(3);
            case FRIDAY -> startDate.minusDays(4);
            case SATURDAY -> startDate.plusDays(2);
            case SUNDAY -> startDate.plusDays(1);
        };
    }

    private ShiftPlanCopy.WorkDay[] findOverlappingWeek(ShiftPlanCopy copy, List<LocalDate> week) {
        Map<Integer, ShiftPlanCopy.WorkDay[]> calendarWeeks = copy.getSimpleCalenderWeeks();

        for (ShiftPlanCopy.WorkDay[] workDays : calendarWeeks.values()) {
            if (Arrays.stream(workDays)
                    .anyMatch(workDay -> week.contains(workDay.getDate()))) return workDays;
        }
        return null;
    }

    private LocalDate getAdjacentFriday() {
        DayOfWeek dayOfWeek = endDate.getDayOfWeek();

        // Das adjustierte Enddatum wird immer auf den Freitag der Woche gesetzt, in die das geparste Enddatum (letzter
        // Tag im Monat) fällt.
        return switch (dayOfWeek) {
            case SUNDAY -> endDate.minusDays(2);
            case SATURDAY -> endDate.minusDays(1);
            case FRIDAY -> endDate;
            case THURSDAY -> endDate.plusDays(1);
            case WEDNESDAY -> endDate.plusDays(2);
            case TUESDAY -> endDate.plusDays(3);
            case MONDAY -> endDate.minusDays(4);
        };
    }
}
