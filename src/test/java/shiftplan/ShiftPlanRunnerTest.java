package shiftplan;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shiftplan.calendar.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShiftPlanRunnerTest {

    private static final String shiftPlanCopy = "/home/stephan/Projekte/Web/shiftplan_serialized.xml";
    private static final String getShiftPlanCopyXSD = "/home/stephan/Projekte/Web";

    private SwapParams swapParams;

    @BeforeEach
    void getSwapParams() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("swap_params.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        swapParams = mapper.readValue(in, SwapParams.class);
    }

    @Test
    void testInconsistentEmployeeParams() {
        ShiftPlanRunner runner = new ShiftPlanRunner();
        assertThrows(ShiftPlanSwapException.class,
                () -> runner.modifyShiftPlan(shiftPlanCopy, getShiftPlanCopyXSD, swapParams));
    }

    @Test
    void testSwapResult() {
        ShiftPlanRunner runner = new ShiftPlanRunner();
        Map<String, Object> dataModel = runner.modifyShiftPlan(shiftPlanCopy, getShiftPlanCopyXSD, swapParams);
        ShiftSwap.SwapResult swapResult = (ShiftSwap.SwapResult) dataModel.get("swapResult");
        assertAll(
                () -> assertEquals(OP_MODE.SWAP, swapResult.getSwapMode()),
                () -> assertEquals(1, swapResult.getUndistributedHoA()),
                () -> assertEquals(2, swapResult.getUndistributedHoB())
        );
    }

    @Test
    void testConverter() {
        ShiftPlanRunner runner = new ShiftPlanRunner();
        Map<String, Object> dataModel = runner.modifyShiftPlan(shiftPlanCopy, getShiftPlanCopyXSD, swapParams);
        Map<String, Shift> shiftPlan = (Map<String, Shift>) dataModel.get("shiftPlan");
        LocalDate testDate = LocalDate.of(2023, 4,3);
        LocalDate testDate2 = LocalDate.of(2023,4,4);
        assertAll(
                () -> assertNull(shiftPlan.get(testDate.toString()).getLateShift()),
                () -> assertEquals(2, shiftPlan.get(testDate.toString()).getEmployeesInHo().length),
                () -> assertNotNull(shiftPlan.get(testDate2.toString()).getLateShift()),
                () -> assertEquals("ID-4", shiftPlan.get(testDate2.toString()).getLateShift().getId())
        );
    }

    @Test
    void testModifyPlan() {
        ShiftPlanRunner runner = new ShiftPlanRunner();
        SwapParams swapParams = runner.getOperationalParams();
        Map<String, Object> dataModel = runner.modifyShiftPlan(shiftPlanCopy, getShiftPlanCopyXSD, swapParams);

        assertAll(
                () -> assertEquals(LocalDate.of(2023,4,1), dataModel.get("startDate")),
                () -> assertEquals(LocalDate.of(2023,6,30), dataModel.get("endDate"))
        );
    }

    @Test
    void testCreatePlanWithPDF() throws TemplateException, IOException {
        ShiftPlanRunner runner = new ShiftPlanRunner();
        SwapParams swapParams = runner.getOperationalParams();
        Map<String, Object> dataModel = runner.createShiftPlan(null, shiftPlanCopy, swapParams);
        Path attachment = runner.createPDF(null, dataModel, System.getProperty("user.home"));
        assertNotNull(attachment);
    }

    @Test
    void testModifyWithPDF() throws TemplateException, IOException {
        ShiftPlanRunner runner = new ShiftPlanRunner();
        SwapParams swapParams = runner.getOperationalParams();
        Map<String, Object> dataModel = runner.modifyShiftPlan(shiftPlanCopy, getShiftPlanCopyXSD, swapParams);
        Path attachment = runner.createPDF(null, dataModel, System.getProperty("user.home"));
        assertNotNull(attachment);
    }
}