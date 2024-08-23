package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.IntStream;

public class ShiftCalendar {

    private static final Logger logger = LogManager.getLogger(ShiftCalendar.class);

    private int calendarForYear;
    public record CalendarInfo(int calendarWeekIndex, LocalDate dateBoundary) {}

    public ShiftCalendar() {}

    public ShiftCalendar(int year) {calendarForYear = year;}

    public LocalDate getFirstCalendarWeekStart(int year) {
        // TODO: Instanz-Methode in statisch Methode abändern
        LocalDate firstDay = LocalDate.of(year,1,1); // 01. Januar des Jahres <year>
        LocalDate firstDayPlusSeven = firstDay.plusDays(7);
        LocalDate firstThursday = firstDay
                .datesUntil(firstDayPlusSeven)
                .filter(date -> date.getDayOfWeek() == DayOfWeek.THURSDAY)
                .findFirst()
                .orElse(null);
        logger.debug("Erster Donnerstag in {}: {}", year, firstThursday);
        return Objects.requireNonNull(firstThursday).minusDays(3);
    }

    int getMaxCalendarWeek(int calendarForYear) {
        int maxIndex = 52;
        LocalDate firstOfJan = LocalDate.of(calendarForYear,1,1);
        DayOfWeek weekDayFirstOfJan = firstOfJan.getDayOfWeek();
        DayOfWeek weekDayLastOfYear = LocalDate.of(calendarForYear, 12,31).getDayOfWeek();
        if (firstOfJan.isLeapYear()) {
            if ((weekDayFirstOfJan == DayOfWeek.WEDNESDAY && weekDayLastOfYear == DayOfWeek.THURSDAY)
                    || (weekDayFirstOfJan == DayOfWeek.THURSDAY && weekDayLastOfYear == DayOfWeek.FRIDAY)) {
                maxIndex = 53;
            }
        } else {
            if (weekDayFirstOfJan == DayOfWeek.THURSDAY && weekDayLastOfYear == DayOfWeek.THURSDAY) {
                maxIndex = 53;
            }
        }
        return maxIndex;
    }

    List<LocalDate[]> createCalendar(int calendarForYear, LocalDate firstDayOfFirstWeek) {
        var ref = new Object() {
            LocalDate start = firstDayOfFirstWeek;
        };

        int rangeEnd = getMaxCalendarWeek(calendarForYear);

        List<LocalDate[]> calendarWeeks = new ArrayList<>();
        IntStream.range(0, rangeEnd).forEach(cwIndex -> {
            logger.trace("*** Kalenderwoche {} im Jahr {} ***", cwIndex +1, ref.start.getYear());
            List<LocalDate> calendarWeekList = ref.start.datesUntil(ref.start.plusDays(7)).toList();
            LocalDate[] calendarWeek = calendarWeekList.toArray(new LocalDate[] {});
            calendarWeeks.add(cwIndex, calendarWeek);
            logger.trace("CW {} von {} bis {}",
                    cwIndex +1, calendarWeek[0], calendarWeek[calendarWeek.length -1]);
            ref.start = calendarWeek[calendarWeek.length -1].plusDays(1);
        });
        return calendarWeeks;
    }

    public Map<Integer, LocalDate[]> createCalendar(LocalDate start, LocalDate end) {
        assert start != null && end != null;

        logger.info("Kalender von {} bis {} wird erstellt",
                start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                end.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));

        LocalDate firstDayInCW = getFirstCalendarWeekStart(calendarForYear);

        CalendarInfo startCw = getCalendarWeekOfDate(firstDayInCW, start, true);
        CalendarInfo endCw = getCalendarWeekOfDate(firstDayInCW, end, false);

        var ref = new Object() {
            LocalDate start = startCw.dateBoundary();
        };

        int rangeEnd = getMaxCalendarWeek(calendarForYear);

        Map<Integer, LocalDate[]> calendarWeeks = new TreeMap<>(Integer::compareTo);
        IntStream.range(0, rangeEnd)
                .takeWhile(cwIndex -> (cwIndex +1) <= endCw.calendarWeekIndex())
                .forEach(cwIndex -> {
            logger.trace("*** Kalenderwoche {} im Jahr {} ***", cwIndex +1, startCw.dateBoundary().getYear());

            // Kalenderwoche liegt vor dem angeforderten Start des Kalenders / Schichtplans
            if ((cwIndex +1) < startCw.calendarWeekIndex) return;

            List<LocalDate> calendarWeekList = ref.start.datesUntil(ref.start.plusDays(7)).toList();
            LocalDate[] calendarWeek = calendarWeekList.toArray(new LocalDate[] {});
            calendarWeeks.put(cwIndex +1, calendarWeek);

            logger.trace("CW {} von {} bis {}",
                    cwIndex +1, calendarWeek[0], calendarWeek[calendarWeek.length -1]);
            ref.start = calendarWeek[calendarWeek.length -1].plusDays(1);
        });

        return calendarWeeks;
    }

    public CalendarInfo getCalendarWeekOfDate(final LocalDate firstDayInCalendar, final LocalDate date, boolean calendarStart) {
        // Ermitteln der Kalenderwoche anhand eines Start- bzw. Enddatums. Die ermittelte Start- und Endkalenderwoche
        // bestimmen die in shiftplan.xml definierte Zeitspanne des Kalenders (z.B. vom 01.04. - 30.09.)
        boolean startDateIsWeekend =
                date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;

        final var calendarInfoWrapper = new Object() {
            CalendarInfo calendarInfo = null;
        };
        final var firstDayInCWWrapper = new Object() {
            LocalDate firstDayInCW = firstDayInCalendar;
        };

        int rangeEnd = getMaxCalendarWeek(calendarForYear);

        IntStream.range(0, rangeEnd)
                .takeWhile(cwIndex -> calendarInfoWrapper.calendarInfo == null)
                .forEach(cwIndex -> {
            logger.debug("Prüfen von Kalenderwoche {}", cwIndex +1);

            List<LocalDate> calendarWeekList =
                            firstDayInCWWrapper.firstDayInCW.datesUntil(firstDayInCWWrapper.firstDayInCW.plusDays(7)).toList();
            LocalDate[] calendarWeek = calendarWeekList.toArray(new LocalDate[] {});
            LocalDate cwStart = calendarWeek[0]; // Montag der jeweiligen KW
            logger.debug("cwStart in KW {}: {}", cwIndex +1, cwStart);
            LocalDate cwEnd = calendarWeek[calendarWeek.length - 1]; // Sonntag der jeweiligen KW
            logger.debug("cwEnd in KW {}: {}", cwIndex +1, cwEnd);

            if ((date.isEqual(cwStart) || date.isAfter(cwStart))
                    && (date.isEqual(cwEnd) || date.isBefore(cwEnd))) {
                if (startDateIsWeekend && calendarStart) {
                    // Wenn das Startdatum <date> auf ein Wochenende fällt, beginnt der Kalender mit der
                    // darauffolgenden Woche, da in der aktuellen Kalenderwoche kein Arbeitstag liegt
                    calendarInfoWrapper.calendarInfo = new CalendarInfo(cwIndex +2, cwEnd.plusDays(1));
                } else {
                    calendarInfoWrapper.calendarInfo = new CalendarInfo(cwIndex + 1, cwStart);
                }
                logger.debug("Kalender {} in KW {} mit Wochenstart-Datum {}",
                        calendarStart ? "startet" : "endet",
                        calendarInfoWrapper.calendarInfo.calendarWeekIndex,
                        calendarInfoWrapper.calendarInfo.dateBoundary);
            }
            firstDayInCWWrapper.firstDayInCW = cwEnd.plusDays(1);
        });
        return calendarInfoWrapper.calendarInfo;
    }

    LocalDate adjustEndDateForDecember(LocalDate calendarEnd) {
        // Der Algorithmus für das Bestimmen der letzten Kalenderwoche des Wochenkalenders in <getCalendarWeekOfDate>
        // funktioniert nicht korrekt, wenn A): Das Enddatum im Dezember liegt und B): Der 31.01., der in diesem Fall
        // in shiftplan.xml normalerweise als das Enddatum des Kalenders angegeben wird, in der KW 1 des Folgejahres
        // liegt.
        // In dieser Konstellation muss das Enddatum auf das letzte Datum korrigiert werden, das in die letzte
        // KW (52 oder 53) fällt.

        // Aufruf dieser Methode nur für die Bestimmung des Kalenderendes im Monat Dezember zulässig!
        assert calendarEnd.isEqual(LocalDate.of(calendarForYear,12,31));

        var calendarEndWrapper = new Object() {
            LocalDate calEnd = calendarEnd;
        };
        LocalDate cwStartInNextYear = getFirstCalendarWeekStart(calendarForYear +1); // KW 1 im Folgejahr
        List<LocalDate> cw1InNextYear = cwStartInNextYear.datesUntil(cwStartInNextYear.plusDays(7)).toList();
        // Prüfen, ob der 31.12. in der ersten KW des Folgejahres liegt.
        LocalDate thirtyOneOfDec = cw1InNextYear
                .stream()
                .filter(date -> date.isEqual(calendarEndWrapper.calEnd))
                .findFirst()
                .orElse(null);
        if (thirtyOneOfDec != null) {
            // Der 31.12. fällt in die KW 1 des Folgejahres. Das Enddatum des Kalenders muss auf den letzten Sonntag
            // des Jahres gesetzt werden.
           if (calendarEnd.getDayOfWeek() == DayOfWeek.WEDNESDAY) {
               calendarEndWrapper.calEnd = calendarEnd.minusDays(3);
           } else if (calendarEnd.getDayOfWeek() == DayOfWeek.TUESDAY) {
               calendarEndWrapper.calEnd  = calendarEnd.minusDays(2);
           } else if (calendarEnd.getDayOfWeek() == DayOfWeek.MONDAY) {
               calendarEndWrapper.calEnd  = calendarEnd.minusDays(1);
           } else {
               throw new IllegalArgumentException(
                       "Das Enddatum des Kalenders muss immer der letzte Tag eines Monats sein");
           }
        }
        logger.info("Enddatum im Dezember (per shiftplan.xml oder adjustiert): {}", calendarEndWrapper.calEnd);
        return calendarEndWrapper.calEnd;
    }

    LocalDate adjustEndDateForDecemberFullWeek(LocalDate adjustedFriday) {
        // Im nicht-strikten Modus bei der Erstellung eines Schichtplans besteht in Bezug auf das Planende am Jahresende
        // das gleiche Problem wie oben: Der extrapolierte Freitag als letzter Tag des Schichtplans am Jahresende
        // kann sich bereits in der KW 1 des Folgejahres befinden. In diesem Fall muss das Schichtplanende um eine
        // Woche, d.h. auf den Freitag der letzten Kalenderwoche im Gültigkeitsjahr des Schichtplans, zurückdatiert
        // werden.
        LocalDate cwStartInNextYear = getFirstCalendarWeekStart(calendarForYear +1);
        assert cwStartInNextYear.getDayOfWeek() == DayOfWeek.MONDAY;

        LocalDate firstFridayOfCW1 = cwStartInNextYear.plusDays(4);
        if (firstFridayOfCW1.isEqual(adjustedFriday)) {
            // Der adjustierte Freitag befindet sich in KW 1 des Folgejahres und muss zurückdatiert werden
            return adjustedFriday.minusDays(7);
        } else if (firstFridayOfCW1.isBefore(adjustedFriday)) {
            // Diese Situation wird wahrscheinlich nie eintreten
            return adjustedFriday.minusDays(14);
        }
        return adjustedFriday;
    }
}
