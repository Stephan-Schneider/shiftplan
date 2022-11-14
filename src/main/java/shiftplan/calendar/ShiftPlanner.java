package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.users.EmployeeGroup;

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

    public void createHomeOfficePlan(List<EmployeeGroup> employeeGroups, int homeOfficeDayCount) {
        assert employeeGroups != null && !employeeGroups.isEmpty();
        int forwardCount = employeeGroups.size() * homeOfficeDayCount;
        logger.debug("forwardCount = {}", forwardCount);
        int startIndex = 0;
        List<LocalDate> workDays = getWorkDays();
        logger.debug("workdays.size() (number of workdays) = {}", workDays.size());

        for (EmployeeGroup employeeGroup : employeeGroups) {
            for (int index = startIndex; index < workDays.size(); index +=forwardCount) {
                int outerBound = checkRangeInBounds(workDays.size(), index, homeOfficeDayCount);
                logger.debug("outerBound: {}", outerBound);
                if (outerBound > -1) {
                    List<LocalDate> dateRange = workDays.subList(index, outerBound);
                    employeeGroup.addToPlan(dateRange);
                }
            }
            startIndex += homeOfficeDayCount;
            logger.debug("startIndex (nach Ende der inneren (for-) Schleife: {}", startIndex);
        }
    }

    private int checkRangeInBounds(int workDaysSize, int currentIndex, int homeOfficeDayCount) {
        for (int i = homeOfficeDayCount; i >= 1; --i) {
            int outerBound = currentIndex + i;
            if (outerBound < workDaysSize) {
                return outerBound;
            }
        }
        return -1;
    }

    List<LocalDate> getWorkDays() {
        List<LocalDate> workDays = new ArrayList<>();
        LongStream allDaysOfYear = LongStream.range(0, startDate.lengthOfYear());
        allDaysOfYear.forEach(dayIncrement -> {
            LocalDate current = startDate.plusDays(dayIncrement);
            if (isWorkday(current) && isNotHoliday(current)) {
                logger.debug("Nächstes Datum in Liste (Kein W/E, kein Feiertag: {}",
                        current.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
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
        assert date != null;
        return !holidays.contains(date);
    }


}
