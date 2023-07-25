package shiftplan.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import shiftplan.calendar.ShiftPlanCopy;
import shiftplan.calendar.ShiftPolicy;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ShiftPlanSerializerTest {

    private static final Logger logger = LogManager.getLogger(ShiftPlanSerializerTest.class);

    private static ShiftPlanCopy copy;

    @BeforeAll
    static void setUp() throws IOException, JDOMException {
        String home = System.getProperty("user.home");
        String xmlPath = Path.of(home, "Projekte", "Web", "shiftplan_serialized.xml").toString();
        logger.debug("xmlPath: {}", xmlPath);

        String xsdPath = Path.of(home, "Projekte", "Web", "shiftplan_serialized.xsd").toString();
        logger.debug("xsdPath: {}", xsdPath);

        ShiftPlanSerializer serializer = new ShiftPlanSerializer();
        copy = serializer.deserializeShiftplan(xmlPath, xsdPath);
    }

    @Test
    void writeXML() throws IOException {
        ShiftPolicy policy = ShiftPolicy.INSTANCE;
        ShiftPolicy.Builder builder = new ShiftPolicy.Builder();
        builder.setMaxHoSlots(2);
        builder.setLateShiftPeriod(4);
        builder.setWeeklyHoCreditsPerEmployee(2);
        builder.setMaxHoDaysPerMonth(8);
        builder.addNoLateShiftOn("MONDAY");
        builder.addNoLateShiftOn("TUESDAY");
        policy.createShiftPolicy(builder);

        ShiftPlanSerializer serializer = new ShiftPlanSerializer();
        Document doc = serializer.serializeShiftPlan(
                2023,
                LocalDate.of(2023,1,1),
                LocalDate.of(2023,12,31),
                policy,
                null,
                null,
                null);
        serializer.writeXML(doc, System.out);
    }

    @Test
    void deserializeXML() throws InvalidShiftPlanException {
        assertEquals(2023, copy.getForYear());
        assertEquals(LocalDate.of(2023,7,1), copy.getFrom());
        assertEquals(LocalDate.of(2023,8,31), copy.getTo());

        ShiftPlanCopy.CalendarWeek calWeek = new ShiftPlanCopy.CalendarWeek(
                27,
                ShiftPlanCopy.CalendarWeek.createCalendarWeekArray(LocalDate.of(
                        2023,7,3), LocalDate.of(2023,7,9)));
        assertNotNull(copy.getCalendarWeeks().get(calWeek));
    }

    @ParameterizedTest
    @MethodSource("createKeys")
    void testShiftPlanCopy(ShiftPlanCopy.CalendarWeek key) {
        Map<ShiftPlanCopy.CalendarWeek, ShiftPlanCopy.WorkDay[]> cws = copy.getCalendarWeeks();
        ShiftPlanCopy.WorkDay[] workDays = cws.get(key);
        for (ShiftPlanCopy.WorkDay workDay : workDays) {
            if (workDay == null) {
                logger.debug("Kein Arbeitstag");
                continue;
            }
            logger.debug("Wochentag: {}", workDay.getDayOfWeek());
            logger.debug("Datum: {}", workDay.getDate());
            logger.debug("isLateShift ? {}", workDay.isLateShift());
            if (workDay.isLateShift()) {
                logger.debug("LateShift (Mitarbeiter): {}", workDay.getLateshift());
            }
            workDay.getEmployeesInHo().forEach(employee -> logger.debug("HO-Office: {}", employee));
        }
    }

    private static Stream<Arguments> createKeys() {
        ShiftPlanCopy.CalendarWeek calWeek1 = new ShiftPlanCopy.CalendarWeek(
                27,
                ShiftPlanCopy.CalendarWeek.createCalendarWeekArray(LocalDate.of(
                        2023,7,3), LocalDate.of(2023,7,9)));

        ShiftPlanCopy.CalendarWeek calWeek2 = new ShiftPlanCopy.CalendarWeek(
                35,
                ShiftPlanCopy.CalendarWeek.createCalendarWeekArray(LocalDate.of(
                        2023,8,28), LocalDate.of(2023,9,3)));
        return Stream.of(
                Arguments.of(calWeek1),
                Arguments.of(calWeek2)
        );
    }


}