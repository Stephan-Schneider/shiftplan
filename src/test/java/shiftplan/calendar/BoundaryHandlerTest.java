package shiftplan.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryHandlerTest {

    private final LocalDate startDate = LocalDate.of(2024,5,1);
    private final LocalDate endDate  = LocalDate.of(2024,6,30);
    private final String xmlFile = "/home/stephan/Projekte/Web/generated_data/shiftplan_serialized.xml";
    private final String xsdPath = "/home/stephan/Projekte/Web/XML";

    @Test
    void getStartDateStrict() {
        BoundaryHandler handler = new BoundaryHandler(startDate, endDate, xmlFile, xsdPath);
        handler.setBoundaryStrict(true);
        LocalDate adjusted = handler.getStartDate();
        assertEquals(startDate, adjusted);
    }

    @Test
    void getStartDateNonStrictNoOverlapping() {
        BoundaryHandler handler = new BoundaryHandler(startDate, endDate, xmlFile, xsdPath);
        LocalDate adjusted = handler.getStartDate();
        assertEquals(LocalDate.of(2024,12,30), adjusted);
    }

    @Test
    void getStartDateNonStrictWithOverlapping() {
        BoundaryHandler handler = new BoundaryHandler(startDate, endDate, xmlFile, xsdPath);
        LocalDate adjusted = handler.getStartDate();
        assertEquals(LocalDate.of(2024,4,29), adjusted);
    }
}