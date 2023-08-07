package shiftplan.calendar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shiftplan.ShiftPlanRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SwapParamsTest {

    private SwapParams swapParams;

    @BeforeEach
    void getObjectMapper() throws IOException {
        /*InputStream in = getClass().getClassLoader().getResourceAsStream("swap_params.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);*/
        ShiftPlanRunner runner = new ShiftPlanRunner();
        swapParams = runner.getOperationalParams();
    }

    @Test
    void testSwapParams() {
        assertAll(
                () -> assertEquals(Path.of("/", "home", "stephan", "Projekte", "Web", "shiftplan_serialized.xml"), swapParams.getShiftplanCopyXMLFile()),
                () -> assertEquals("REPLACE", swapParams.getMode().toString()),
                () -> assertEquals(2, swapParams.getEmployeeSet1().length),
                () -> assertEquals(1, swapParams.getEmployeeSet2().length)

        );
    }

    @Test
    void testSwapParamAmbiguities() {
        assertAll(
                () -> assertEquals(1, swapParams.getEmployeeSet2().length),
                () -> assertEquals("CREATE", swapParams.getMode().toString())

        );
    }

}