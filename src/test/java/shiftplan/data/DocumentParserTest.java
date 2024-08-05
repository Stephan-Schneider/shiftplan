package shiftplan.data;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import shiftplan.calendar.ShiftPolicy;
import shiftplan.data.xml.ShiftPlanDescriptor;
import shiftplan.users.Employee;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {

    @Test
    void testStartEndDate() throws IOException, JDOMException {
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();
        assertAll(
                () -> assertEquals(LocalDate.of(2023,4,1), descriptor.getStartDate()),
                () -> assertEquals(LocalDate.of(2023,6,30), descriptor.getEndDate())
        );
    }

    @Test
    void checkParsedPolicy() throws IOException, JDOMException {
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();
        ShiftPolicy policy = ShiftPolicy.INSTANCE;

        assertAll(
                () -> assertEquals(4, policy.getLateShiftPeriod()),
                () -> assertEquals(2, policy.getMaxHoSlots()),
                () -> assertEquals(8, policy.getMaxHoDaysPerMonth()),
                () -> assertTrue(policy.getNoLateShiftOn().contains(DayOfWeek.MONDAY))
        );
    }

    @Test
    void checkParsedHolidays() throws IOException, JDOMException {
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();
        List<LocalDate> holidays = descriptor.getHolidays();
        assertAll(
                () -> assertTrue(holidays.size() > 0),
                () -> assertTrue(holidays.contains(LocalDate.of(2023, 5, 29)))
        );
    }

    @Test
    void checkParsedEmployees() throws IOException, JDOMException {
        List<Employee.PARTICIPATION_SCHEMA> partSchemas = List.of(
                Employee.PARTICIPATION_SCHEMA.HO,
                Employee.PARTICIPATION_SCHEMA.LS,
                Employee.PARTICIPATION_SCHEMA.HO_LS
        );
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();
        assertEquals(6, descriptor.getEmployees().length);
        Employee[] employees = descriptor.getEmployees();
        for (Employee employee : employees) {
            assertAll(
                    () -> assertTrue(employee.getId().startsWith("ID")),
                    () -> assertTrue(partSchemas.contains(employee.getParticipationSchema()))
            );
        }
    }

    @Test
    void buildDocWithXSDValidation() throws IOException, JDOMException {
        ShiftPlanDescriptor descriptor = new ShiftPlanDescriptor();
        descriptor.parseDocument();
    }
}