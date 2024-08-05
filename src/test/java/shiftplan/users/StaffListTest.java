package shiftplan.users;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shiftplan.calendar.ShiftCalendar;
import shiftplan.calendar.ShiftPlanCopy;
import shiftplan.data.ShiftPlanSerializer;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StaffListTest {

    private static final Logger logger = LogManager.getLogger(StaffListTest.class);

    private ShiftPlanCopy copy;

    @BeforeEach
    void setUp() throws IOException, JDOMException {
        //String shiftPlanCopyXMLFile = "/home/stephan/Projekte/Web/shiftplan_serialized.xml";
        //String shiftPlanCopySchemaDir = "/home/stephan/Projekte/Web";
        String shiftPlanCopyXMLFile = "/home/stephan/Projekte/Web/generated_data/shiftplan_serialized.xml";
        String shiftPlanCopySchemaDir = "/home/stephan/Projekte/Web/XML";
        ShiftPlanSerializer serializer = new ShiftPlanSerializer(shiftPlanCopyXMLFile, shiftPlanCopySchemaDir);
        copy = serializer.deserializeShiftplan();
    }

    @Test
    void getCurrentCalendarWeek() {
        StaffList staffList = new StaffList(copy, "");
        ShiftCalendar.CalendarInfo calendarInfo = staffList.getCurrentCalendarWeek();
        assertEquals(1, calendarInfo.calendarWeekIndex());
    }

    @Test
    void createStaffList() {
        StaffList staffList = new StaffList(copy, "");
        Map<String, StaffList.StaffData> employeeMap = staffList.createStaffList();
        assertEquals(5, employeeMap.size());
        assertTrue(employeeMap.get("ID-1").cwIndices().contains(36));
    }

    @Test
    void printStaffList() throws IOException {
        StaffList staffList = new StaffList(copy, "/home/stephan");
        Map<String, StaffList.StaffData> employeeList = staffList.createStaffList();
        String printed = staffList.printStaffList(employeeList);
        assertNotNull(printed);
        logger.debug(printed);
        staffList.writeToFile(printed);
    }

    @Test
    void serializeStaffList() {
        StaffList staffList = new StaffList(copy);
        Map<String, StaffList.StaffData> employeeMap = staffList.createStaffList();
        String serialized = staffList.serializeStaffList(employeeMap);
        assertNotNull(serialized);
        logger.debug(serialized);
    }
}