package shiftplan.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import shiftplan.ShiftPlanRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SwapParamsTest {

    private SwapParams swapParams;

    @BeforeEach
    void getSwapParams() throws IOException {
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

    @ParameterizedTest
    @MethodSource("getParamStrings")
    void testFromString(String paramString, int lengthOfEmp2Set) {
        SwapParams swapParams = SwapParams.readFromString(paramString);
        assertAll(
                () -> assertNotNull(swapParams),
                () -> assertEquals(lengthOfEmp2Set, swapParams.getEmployeeSet2().length),
                () -> assertEquals("ID-1", swapParams.getEmployeeSet1()[0]),
                () -> assertEquals("14", swapParams.getEmployeeSet1()[1]),
                () -> assertTrue(swapParams.getMode() == OP_MODE.SWAP || swapParams.getMode() == OP_MODE.REPLACE)

        );
    }

    private static Stream<Arguments> getParamStrings() {
        return Stream.of(
                Arguments.of("SWAP, true,ID-1,14,  ID-4, 16", 2),
                Arguments.of("REPLace, false, ID-1, 14, ID-4", 1)
        );
    }

    @Test
    void testFromStringCreateOnly() {
        String paramString = "CREATE";
        SwapParams swapParams = SwapParams.readFromString(paramString);
        assertSame(OP_MODE.CREATE, swapParams.getMode());
    }

}