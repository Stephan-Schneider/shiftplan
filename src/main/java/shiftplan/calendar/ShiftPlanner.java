package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

public class ShiftPlanner {

    private static final Logger logger = LogManager.getLogger(ShiftPlanner.class);
    private static List<LocalDate> holidays;
    private final LocalDate startDate;

    public ShiftPlanner(LocalDate startDate) {
        this.startDate = startDate;
        holidays = List.of(
                LocalDate.of(2022, 1, 1), // Neujahr
                LocalDate.of(2022, 4, 15), // Karfreitag
                LocalDate.of(2022, 4, 18), // Ostermontag
                LocalDate.of(2022, 6,6), // Pfingstmontag
                LocalDate.of(2022, 10, 3), // Tag der Einheit
                LocalDate.of(2022, 12,26) // 2. Weihnachtstag
        );
    }

    List<LocalDate> getWorkDays() {
        List<LocalDate> workDays = new ArrayList<>();
        LongStream allDaysOfYear = LongStream.range(0, startDate.lengthOfYear());
        allDaysOfYear.forEach(dayIncrement -> {
            LocalDate current = startDate.plusDays(dayIncrement);
            logger.debug("Nächstes Datum: {}",
                    current.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
            if (isWorkday(current) && isNotHoliday(current)) {
                workDays.add(current);
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
        return !holidays.contains(date);
    }


}
