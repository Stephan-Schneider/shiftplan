package shiftplan.calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import shiftplan.data.ShiftPlanSerializer;
import shiftplan.users.Employee;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ShiftSwapTest {

    private static final Logger logger = LogManager.getLogger(ShiftSwapTest.class);

    private static ShiftPlanCopy copy;
    private ShiftSwap swapper;

    @BeforeAll
    static void setUp() throws IOException, JDOMException {
        String home = System.getProperty("user.home");
        Path xmlPath = Path.of(home, "Projekte", "Web", "shiftplan_serialized.xml");
        logger.debug("xmlPath: {}", xmlPath);

        Path xsdPath = Path.of(home, "Projekte", "Web", "shiftplan_serialized.xsd");
        logger.debug("xsdPath: {}", xsdPath);

        ShiftPlanSerializer serializer = new ShiftPlanSerializer(xmlPath, xsdPath);
        copy = serializer.deserializeShiftplan();
    }

    @BeforeEach
    void initSwapper() {
        swapper = new ShiftSwap(copy);
    }

    @ParameterizedTest
    @MethodSource("getIndices")
    void testCalendarWeekIndexIsValid(int index, boolean isValid) {
        assertEquals(isValid, swapper.indexOutOfRange(index));
    }

    private static Stream<Arguments> getIndices() {
        return Stream.of(
                Arguments.of(1, true),
                Arguments.of(15, false),
                Arguments.of(27, true),
                Arguments.of(35, true),
                Arguments.of(36, true)
        );
    }

    @ParameterizedTest
    @MethodSource("getEmployeeIds")
    void testEmployeeIDs(String id, boolean isNull) {
        assertEquals(isNull, swapper.employeeIsNull(copy.getEmployeeById(id)));
    }

    private static Stream<Arguments> getEmployeeIds() {
        return Stream.of(
                Arguments.of("ID-1", false),
                Arguments.of("ID-6", false),
                Arguments.of("iD-10", true),
                Arguments.of("id-1", true)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgsForIsLateShiftTest")
    void testIsLateShift(String employeeId, int cwIndex, boolean isLateShift) {
        Map<Integer, ShiftPlanCopy.WorkDay[]> calendarWeeks = swapper.getSimpleCalendarWeeks();
        assertEquals(isLateShift, swapper.isLateshift(copy.getEmployeeById(employeeId), cwIndex));
    }

    private static Stream<Arguments> getArgsForIsLateShiftTest() {
        return Stream.of(
                Arguments.of("ID-1", 27, true),
                Arguments.of("ID-2", 28, false), // Part.-Schema: HO
                Arguments.of("ID-1", 29, false) // Ungültiger CW-Index
        );
    }

    @Test
    void testSortedCalendarWeeks() {
        List<ShiftPlanCopy.CalendarWeek> sorted = swapper.getCalendarWeeks();
        assertAll(
                () -> assertEquals(27, sorted.get(0).cwIndex()),
                () -> assertEquals(28, sorted.get(1).cwIndex()),
                () -> assertEquals(35, sorted.get(sorted.size() -1).cwIndex())
        );
    }

    @ParameterizedTest
    @MethodSource("getParamsForTestGetIndexOfFirstLateShift")
    void testGetIndexOfFirstLateShift(int cwIndex, String employeeId, int expectedIndex) {
        ShiftPlanCopy.WorkDay[] workDays = swapper.getSimpleCalendarWeeks().get(cwIndex);
        Employee employee = copy.getEmployeeById(employeeId);
        assertEquals(expectedIndex, swapper.getIndexOfFirstLateShift(workDays, employee));

    }

    private static Stream<Arguments> getParamsForTestGetIndexOfFirstLateShift() {
        return Stream.of(
                Arguments.of(14, "ID-1", 1),
                Arguments.of(15, "ID-1", 0),
                Arguments.of(15, "ID-3", 1),
                Arguments.of(16, "ID-4", 2)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForTestSwap")
    void testSwap(String employee1Id, int cwIndex1, String employee2Id, int cwIndex2, Employee emp1, Employee emp2) {
        swapper.swap(employee1Id, cwIndex1, employee2Id, cwIndex2);
        Map<Integer, ShiftPlanCopy.WorkDay[]> cal = swapper.getSimpleCalendarWeeks();

        assertAll(
                () -> assertEquals(emp2, cal.get(cwIndex1)[1].getLateshift()),
                () -> assertEquals(emp2, cal.get(cwIndex1 +1)[0].getLateshift()),
                () -> assertEquals(emp1, cal.get(cwIndex2)[2].getLateshift()),
                () -> assertEquals(emp1, cal.get(cwIndex2 +1)[1].getLateshift())
                //() -> assertEquals(emp2, cal.get(cwIndex1)[2].getLateshift()),
                //() -> assertEquals(emp2, cal.get(cwIndex1 +1)[1].getLateshift()),
                //() -> assertEquals(emp1, cal.get(cwIndex2)[3].getLateshift()),
                //() -> assertEquals(emp1, cal.get(cwIndex2 +1)[0].getLateshift())
        );
    }


    @ParameterizedTest
    @MethodSource("getArgumentsForTestSwap")
    void testCancelledHoDays(String employee1Id, int cwIndex1, String employee2Id, int cwIndex2, Employee emp1, Employee emp2) {
        Map<String, Integer> cancelledHoDays = swapper.swapLateShift(emp1, cwIndex1, emp2, cwIndex2);
        assertAll(
                () -> assertEquals(2, cancelledHoDays.get(employee1Id)),
                () -> assertEquals(2, cancelledHoDays.get(employee2Id))
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForTestSwap")
    void testSwapHomeOfficeDays(String employee1Id, int cwIndex1, String employee2Id, int cwIndex2, Employee emp1, Employee emp2) {
        Map<String, Integer> cancelledHoDays = swapper.swapLateShift(emp1, cwIndex1, emp2, cwIndex2);
        int remainingHoDays1 = swapper.swapHomeOfficeDays(emp1, emp2, cwIndex1, cancelledHoDays.get(employee1Id));
        int remainingHoDays2 = swapper.swapHomeOfficeDays(emp2, emp1, cwIndex2, cancelledHoDays.get(employee2Id));

        assertAll(
                () -> assertEquals(1, remainingHoDays1),
                () -> assertEquals(2, remainingHoDays2),
                () -> assertTrue(swapper.getSimpleCalendarWeeks().get(cwIndex1)[2].hasHoDay(emp1)),
                () -> assertFalse(swapper.getSimpleCalendarWeeks().get(cwIndex1)[3].hasHoDay(emp1)),
                () -> assertEquals(1, swapper.getSimpleCalendarWeeks().get(cwIndex1)[3].getEmployeesInHo().size())
        );
    }

    private static Stream<Arguments> getArgumentsForTestSwap() {
        return Stream.of(
                Arguments.of("ID-1", 14, "ID-4", 16, copy.getEmployeeById("ID-1"), copy.getEmployeeById("ID-4"))//,
                //Arguments.of("ID-3", 20, "ID-4", 21, copy.getEmployeeById("ID-3"), copy.getEmployeeById("ID-4"))
        );
    }

    @Test
    void testHOCancellation() {
        Employee testEmployee = copy.getEmployeeById("ID-1");
        Map<Integer, ShiftPlanCopy.WorkDay[]> calendar = swapper.getSimpleCalendarWeeks();
        ShiftPlanCopy.WorkDay workDay = calendar.get(14)[0];
        assertAll(
                () -> assertEquals(0, workDay.getFreeSlots()),
                () -> assertEquals(1, workDay.removeEmployeeInHo(testEmployee)),
                () -> assertEquals(1, workDay.getFreeSlots())
        );
    }

    @ParameterizedTest
    @MethodSource("getArgsForTestGetHoCreditsPerWeek")
    void testGetHOCreditsPerWeek(ShiftPlanCopy.WorkDay[] week, Employee hoCandidate, int expectedResult) {
        assertEquals(expectedResult, swapper.openHoCreditsInWeek(week, hoCandidate));
    }

    private static Stream<Arguments> getArgsForTestGetHoCreditsPerWeek() {
        ShiftSwap shiftSwap = new ShiftSwap(copy);
        Map<Integer, ShiftPlanCopy.WorkDay[]> cal = shiftSwap.getSimpleCalendarWeeks();
        return Stream.of(
                Arguments.of(cal.get(14), copy.getEmployeeById("ID-1"), 1),
                Arguments.of(cal.get(14), copy.getEmployeeById("ID-6"), 0),
                Arguments.of(cal.get(17), copy.getEmployeeById("ID-2"), 0)
        );
    }

    @Test
    void testReplace() {
        Employee replaced = copy.getEmployeeById("ID-1");
        Employee replacer = copy.getEmployeeById("ID-4");
        int cwIndex = 14;

        int cancelledHoDays = swapper.replaceLateShift(replaced, replacer, 14);
        Map<Integer, ShiftPlanCopy.WorkDay[]> cal = swapper.getSimpleCalendarWeeks();

        assertAll(
                () -> assertEquals(2, cancelledHoDays),
                () -> assertEquals(replacer, cal.get(cwIndex)[1].getLateshift()),
                () -> assertEquals(replacer, cal.get(cwIndex +1)[0].getLateshift())
        );
    }
}