package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
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
}