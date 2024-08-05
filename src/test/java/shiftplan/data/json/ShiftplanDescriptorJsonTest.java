package shiftplan.data.json;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import shiftplan.calendar.ShiftPolicy;
import shiftplan.users.Employee;
import shiftplan.web.ConfigBundle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShiftplanDescriptorJsonTest {

    private ShiftplanDescriptorJson descriptor;

    @BeforeAll
    void setUp() {
        new ConfigBundle.ConfigBuilder(
                "/home/stephan/Projekte/Web/generated_data/shiftplan_serialized",
                "/home/stephan/Projekte/Web/XML")
                .templateDir("/home/stephan/Projekte/Web/Template")
                .generatedDataDir("/home/stephan/Projekte/Web/generated_data")
                .smtpConfigPath("/home/stephan/Projekte/Web")
                .jsonFile("/home/stephan/Projekte/Web/shiftplan_test.json")
                .build();
        descriptor = ShiftplanDescriptorJson.readObject();
    }

    @Test
    void getYear() {
        assertEquals(2024, descriptor.getYear());
    }

    @Test
    void getStartDate() {
        assertEquals(LocalDate.of(2024, 1,1), descriptor.getStartDate());
    }

    @Test
    void getEndDate() {
        assertEquals(LocalDate.of(2024,6,30), descriptor.getEndDate());
    }

    @Test
    void getHolidays() {
        assertAll("Test holidays",
                () -> {
                    List<LocalDate> holidays = descriptor.getHolidays();
                    assertEquals(10, holidays.size());

                    assertAll("holidays.get(0)",
                            () -> assertEquals(LocalDate.of(2024,1,1), holidays.get(0)));
                }
        );
    }

    @Test
    void getEmployees() {
        assertAll("Test employees",
                () -> {
                    Employee[] employees = descriptor.getEmployees();
                    assertEquals(7, employees.length);

                    Employee employee = employees[0];
                    assertAll("Test employee[0]",
                            () -> assertEquals("ID-1", employee.getId()),
                            () -> assertEquals("Rudi", employee.getName()),
                            () -> assertEquals("Ratlos", employee.getLastName()),
                            () -> assertEquals(1, employee.getBackups().size()),
                            () -> assertEquals("ID-3", employee.getBackups().get(0).getId())
                    );
                });
    }

    @Test
    void createPolicy() {
        ShiftPolicy policy = ShiftPolicy.INSTANCE;

        assertAll("Test ShiftPolicy",
                () -> assertEquals(4, policy.getLateShiftPeriod()),
                () -> assertEquals(10, policy.getMaxHoDaysPerMonth()),
                () -> assertEquals(2, policy.getNoLateShiftOn().size()),
                () -> assertEquals(3, policy.getWeeklyHoCreditsPerEmployee()),
                () -> assertEquals(3, policy.getMaxHoSlots()),
                () -> assertEquals(2, policy.getMaxSuccessiveHODays()),
                () -> assertEquals(2, policy.getMinDistanceBetweenHOBlocks()),
                () -> assertEquals(DayOfWeek.MONDAY, policy.getNoLateShiftOn().get(0))
        );
    }
}