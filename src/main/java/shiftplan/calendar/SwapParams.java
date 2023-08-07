package shiftplan.calendar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.ShiftPlanRunnerException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

@JsonIgnoreProperties({"mode_comment1", "mode_comment2", "employee_comment1", "employee_comment2", "swapHomeOffice_comment"})
public class SwapParams {

    private static final Logger logger = LogManager.getLogger(SwapParams.class);

    private OP_MODE mode;
    private Path shiftplanCopyXMLFile;
    private Path shiftPlanCopySchemaDir;
    private String[] employeeSet1;
    private String[] employeeSet2;
    private boolean swapHo;

    public static SwapParams readSwapParams(String... path) throws ShiftPlanRunnerException {
        logger.info("Die Parameter werden ausgelesen");
        logger.debug("path: {}", Arrays.toString(path));

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        if (path == null || path.length == 0) {
            // Die Parameterdatei im Ordner 'resources' während der Entwicklungsphase oder im JAR-Archiv wird
            // verwendet
            return readFromInputStream(mapper);
        } else {
            // Die Parameterdatei im Installationsverzeichnis oder der explizit bei Aufruf des Programms
            // als Argument angegebene Pfad wird verwendet
            return readFromFile(mapper, path[0]);
        }

    }

    private static SwapParams readFromInputStream(ObjectMapper mapper) {
        assert mapper != null;
        logger.info("Die Parameterdatei wird aus einem InputStream gelesen");
        InputStream in = SwapParams.class.getClassLoader().getResourceAsStream("swap_params.json");
        if (in == null) {
            throw new ShiftPlanRunnerException("Die Parameterdatei 'swap_params.json' nicht gefunden!" );
        }
        try {
            return mapper.readValue(in, SwapParams.class);
        } catch (IOException ex) {
            logger.error("Ausnahme beim Zugriff auf swap_params.json", ex);
            throw new ShiftPlanRunnerException(ex.getMessage());
        }
    }

    private static SwapParams readFromFile(ObjectMapper mapper, String pathToParamFile) {
        // <pathToParamFile> enthält den vollständigen Pfad zur Parameterdatei. Der voreingestellte Name dieser
        // Datei ist 'swap_params.json'. Falls der Dateiname geändert wird, muss der neue Pfad (inklusive dem neuen
        // Dateinamen) in shiftplan.sh angegeben werden.
        assert mapper != null;
        logger.info("Die Parameterdatei wird aus einem File-Objekt gelesen");
        Path path = Path.of(pathToParamFile);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ShiftPlanRunnerException(
                    "Der Dateipfad zur Parameterdatei (swap_params.json) '" + pathToParamFile +"' ist ungültig");
        }
        try {
            return mapper.readValue(path.toFile(), SwapParams.class);
        } catch (IOException ex) {
            logger.error("Ausnahme beim Zugriff auf Parameterdatei (swap_params.json)", ex);
            throw new ShiftPlanRunnerException(ex.getMessage());
        }
    }


    public SwapParams() {}

    public OP_MODE getMode() {
        return mode;
    }

    public void setMode(OP_MODE mode) {
        this.mode = mode;
    }

    public Path getShiftplanCopyXMLFile() {
        return shiftplanCopyXMLFile;
    }

    public void setShiftplanCopyXMLFile(Path copy) {
        this.shiftplanCopyXMLFile = copy;
    }

    public Path getShiftPlanCopySchemaDir() {
        return shiftPlanCopySchemaDir;
    }

    public void setShiftPlanCopySchemaDir(Path shiftPlanCopySchemaDir) {
        this.shiftPlanCopySchemaDir = shiftPlanCopySchemaDir;
    }

    @JsonProperty("employeeA")
    public String[] getEmployeeSet1() {
        String[] value = Objects.requireNonNull(employeeSet1,"Fehlende employeeA - Daten!");
        Objects.checkIndex(1, value.length);
        return employeeSet1;
    }

    public void setEmployeeSet1(String[] employeeSet1) {
        this.employeeSet1 = employeeSet1;
    }

    @JsonProperty("employeeB")
    public String[] getEmployeeSet2() {
        String[] value = Objects.requireNonNull(employeeSet2,"Fehlende employeeB - Daten");
        Objects.checkIndex(0, value.length);
        if (mode == OP_MODE.SWAP) Objects.checkIndex(1, value.length);
        return employeeSet2;
    }

    public void setEmployeeSet2(String[] employeeSet2) {
        this.employeeSet2 = employeeSet2;
    }

    @JsonProperty("swapHomeOffice")
    public boolean isSwapHo() {
        return swapHo;
    }

    public void setSwapHo(boolean swapHo) {
        this.swapHo = swapHo;
    }
}
