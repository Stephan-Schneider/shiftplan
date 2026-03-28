package shiftplan.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryHandlerTest {

    private final LocalDate startDate = LocalDate.of(2024,1,1);
    private final LocalDate endDate  = LocalDate.of(2024,4,30);
    private final String xmlFile = "/home/stephan/Projekte/Web/generated_data/shiftplan_serialized.xml";

    @Test
    void getStartDateStrict() {
        BoundaryHandler handler = new BoundaryHandler(startDate, endDate, xmlFile);
        handler.setBoundaryStrict(true);
        LocalDate adjusted = handler.getStartDate();
        assertEquals(startDate, adjusted);
    }

    @Test
    void getStartDateNonStrictNoOverlapping() {
        BoundaryHandler handler = new BoundaryHandler(startDate, endDate, xmlFile);
        LocalDate adjusted = handler.getStartDate();
        assertEquals(LocalDate.of(2024,12,30), adjusted);
    }

    @Test
    void getStartDateNonStrictWithOverlapping() {
        BoundaryHandler handler = new BoundaryHandler(startDate, endDate, xmlFile);
        LocalDate adjusted = handler.getStartDate();
        assertEquals(LocalDate.of(2024,4,29), adjusted);
    }

    @Test
    void getEndDateStrict() {
        BoundaryHandler handler = new BoundaryHandler(startDate, endDate, xmlFile);
        handler.setBoundaryStrict(true);
        LocalDate adjusted = handler.getEndDate();
        assertEquals(endDate, adjusted);
    }

    @Test
    void getEndDateNonStrict() {
        BoundaryHandler handler = new BoundaryHandler(startDate, endDate, xmlFile);
        LocalDate adjusted = handler.getEndDate();
        assertEquals(LocalDate.of(2024,5,3), adjusted);
    }
}