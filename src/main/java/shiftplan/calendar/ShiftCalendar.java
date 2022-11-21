package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class ShiftCalendar {

    private static final Logger logger = LogManager.getLogger(ShiftCalendar.class);

    LocalDate getFirstCalendarWeekStart(int year) {
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
}
