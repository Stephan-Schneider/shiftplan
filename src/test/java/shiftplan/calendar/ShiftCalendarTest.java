package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ShiftCalendarTest {

    private static final Logger logger = LogManager.getLogger(ShiftCalendar.class);

    @ParameterizedTest
    @ValueSource(ints = {2018,2019,2020,2021,2022,2023,2024})
    void getFirstCalendarWeekStart(int year) {
        ShiftCalendar shiftCalendar = new ShiftCalendar();
        LocalDate startOfFirstWeek = shiftCalendar.getFirstCalendarWeekStart(year);
        logger.debug("Erster Tag der KW 1: {}", startOfFirstWeek);
        assertAll("CW start tests",
                () -> assertFalse(startOfFirstWeek.isBefore(LocalDate.of(year - 1, 12, 29))),
                () -> assertFalse(startOfFirstWeek.isAfter(LocalDate.of(year, 1, 4))),
                () -> assertSame(startOfFirstWeek.getDayOfWeek(), DayOfWeek.MONDAY)
        );
    }

    @ParameterizedTest
    @MethodSource("getValuesForMaxIndex")
    void getCalenderWeekMaxIndex(int year, int maxIndex) {
        ShiftCalendar shiftCalendar = new ShiftCalendar();
        int result = shiftCalendar.getMaxCalendarWeek(year);
        assertEquals(maxIndex, result);
    }

    private static Stream<Arguments> getValuesForMaxIndex() {
        return Stream.of(
                Arguments.of(2019, 52),
                Arguments.of(2020, 53),
                Arguments.of(2021, 52)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {2019,2020,2021})
    void createCalendarWeeks(int year) {
        ShiftCalendar shiftCalendar = new ShiftCalendar();
        LocalDate startOfFirstWeek = shiftCalendar.getFirstCalendarWeekStart(year);
        logger.debug("Erster Tag der KW 1: {}", startOfFirstWeek);

        List<LocalDate[]> calendar = shiftCalendar.createCalendar(year, startOfFirstWeek);
    }

    @ParameterizedTest
    @MethodSource("getInputForCreateCalendarWeeksWithStartEndDate")
    void createCalendarWeeksWithStartEndDate(int year, LocalDate start, LocalDate end) {
        ShiftCalendar shiftCalendar = new ShiftCalendar(year);
        Map<Integer, LocalDate[]> calendarWeeks = shiftCalendar.createCalendar(start, end);
        assertFalse(calendarWeeks.isEmpty());

        for (Integer key : calendarWeeks.keySet()) {
            logger.debug("Kalenderwoche {} / Beginn {}, Ende {}",
                    key,
                    calendarWeeks.get(key)[0],
                    calendarWeeks.get(key)[6]);
        }
    }

    private static Stream<Arguments> getInputForCreateCalendarWeeksWithStartEndDate() {
        return Stream.of(
                Arguments.of(2023, LocalDate.of(2023,3,1), LocalDate.of(2023,9,30)),
                Arguments.of(2023, LocalDate.of(2023,1,2), LocalDate.of(2023,12,31)),
                Arguments.of(2023, LocalDate.of(2023,4,1), LocalDate.of(2023,10,31)),
                Arguments.of(2024, LocalDate.of(2024,3,1), LocalDate.of(2024,12,31)),
                Arguments.of(2025, LocalDate.of(2025,1,1), LocalDate.of(2025,12,31))

        );
    }

    @ParameterizedTest
    @MethodSource("getStartDates")
    void getCalendarStart(int year, LocalDate startDate) {
        ShiftCalendar shiftCalendar = new ShiftCalendar(year);

        LocalDate firstDayInCW = shiftCalendar.getFirstCalendarWeekStart(year);
        logger.debug("firstDayInCW: {}", firstDayInCW);

        if (startDate.getMonth() == Month.JANUARY) {
            if (startDate.isBefore(firstDayInCW)) {
                startDate = firstDayInCW;
            }
        }

        ShiftCalendar.CalendarInfo calendarStart = shiftCalendar.getCalendarWeekOfDate(
                firstDayInCW, startDate, true);
        assertNotNull(calendarStart);
    }

    @ParameterizedTest
    @MethodSource("getEndDates")
    void getCalendarEnd(int year, LocalDate endDate) {
        ShiftCalendar shiftCalendar = new ShiftCalendar(year);

        if (endDate.getMonth() == Month.DECEMBER) {
            endDate = shiftCalendar.adjustEndDateForDecember(endDate);
        }

        LocalDate firstDayInCW = shiftCalendar.getFirstCalendarWeekStart(year);
        logger.debug("firstDayInCW: {}", firstDayInCW);

        ShiftCalendar.CalendarInfo calendarEnd = shiftCalendar.getCalendarWeekOfDate(
                firstDayInCW, endDate, false);
        assertNotNull(calendarEnd);
    }

    private static Stream<Arguments> getStartDates() {
        return Stream.of(
                Arguments.of(2018, LocalDate.of(2018,1,1)),
                Arguments.of(2019, LocalDate.of(2019,1,1)),
                Arguments.of(2020, LocalDate.of(2020,1,1)),
                Arguments.of(2021, LocalDate.of(2021,1,1)),
                Arguments.of(2022, LocalDate.of(2022,1,1)),
                Arguments.of(2023, LocalDate.of(2023, 1,1)),
                Arguments.of(2024, LocalDate.of(2024,1,1)),
                Arguments.of(2025, LocalDate.of(2025,1,1)),
                Arguments.of(2026, LocalDate.of(2026,1,1)),
                Arguments.of(2027, LocalDate.of(2027,1,1)),
                Arguments.of(2028, LocalDate.of(2028,1,1)),
                Arguments.of(2023, LocalDate.of(2023, 4,1)),
                Arguments.of(2023,LocalDate.of(2023,6,1)),
                Arguments.of(2023,LocalDate.of(2023,10,1)),
                Arguments.of(2023, LocalDate.of(2023, 9,1))
        );
    }

    private static Stream<Arguments> getEndDates() {
        return Stream.of(
                Arguments.of(2018, LocalDate.of(2018,12,31)),
                Arguments.of(2019, LocalDate.of(2019,12,31)),
                Arguments.of(2020, LocalDate.of(2020,12,31)),
                Arguments.of(2021, LocalDate.of(2021,12,31)),
                Arguments.of(2022, LocalDate.of(2022,12,31)),
                Arguments.of(2023, LocalDate.of(2023, 12,31)),
                Arguments.of(2024, LocalDate.of(2024,12,31)),
                Arguments.of(2025, LocalDate.of(2025,12,31)),
                Arguments.of(2026, LocalDate.of(2026,12,31)),
                Arguments.of(2027, LocalDate.of(2027,12,31)),
                Arguments.of(2028, LocalDate.of(2028,12,31)),
                Arguments.of(2023, LocalDate.of(2023, 4,30)),
                Arguments.of(2023,LocalDate.of(2023,7,31)),
                Arguments.of(2023,LocalDate.of(2023,10,31))
        );
    }

    @ParameterizedTest
    @MethodSource("getEndDates")
    void calendarEndTest(int year, LocalDate endDate) {
        ShiftCalendar shiftCalendar = new ShiftCalendar(year);
        LocalDate adjustedEndDateForDecember = shiftCalendar.adjustEndDateForDecember(endDate);
    }
}