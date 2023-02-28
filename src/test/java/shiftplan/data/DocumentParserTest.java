package shiftplan.data;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import shiftplan.users.EmployeeGroup;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {

    @Test
    void parseDocument() throws IOException, JDOMException {
        DocumentParser documentParser = new DocumentParser();
        documentParser.parseDocument();
        assertEquals(2023, documentParser.getYear());
        assertTrue(documentParser.getHolidays().size() > 0);
        assertEquals(3, documentParser.getEmployeeGroupList().size());
        List<EmployeeGroup> employeeGroups = documentParser.getEmployeeGroupList();
        for (EmployeeGroup employeeGroup : employeeGroups) {
            assertTrue(employeeGroup.getGroupName().startsWith("Gruppe"));
            assertEquals(2, employeeGroup.getEmployees().length);
        }
    }

    @Test
    void buildDocWithXSDValidation() throws IOException, JDOMException {
        DocumentParser documentParser = new DocumentParser();
        documentParser.parseDocument();
    }
}