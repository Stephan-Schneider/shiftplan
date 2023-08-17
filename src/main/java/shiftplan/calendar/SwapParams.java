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
import java.util.stream.IntStream;

@JsonIgnoreProperties({"mode_comment1", "mode_comment2", "employee_comment1", "employee_comment2", "swapHomeOffice_comment"})
public class SwapParams {

    private static final Logger logger = LogManager.getLogger(SwapParams.class);

    private OP_MODE mode;
    private String[] employeeSet1;
    private String[] employeeSet2;
    private boolean swapHo;

    public static SwapParams readSwapParams(Path... path) throws ShiftPlanRunnerException {
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

    private static SwapParams readFromFile(ObjectMapper mapper, Path pathToParamFile) {
        // <pathToParamFile> enthält den vollständigen Pfad zur Parameterdatei. Der voreingestellte Name dieser
        // Datei ist 'swap_params.json'. Falls der Dateiname geändert wird, muss der neue Pfad (inklusive dem neuen
        // Dateinamen) in shiftplan.sh angegeben werden.
        assert mapper != null;
        logger.info("Die Parameterdatei wird aus einem File-Objekt gelesen");
        if (!Files.isRegularFile(pathToParamFile) || !Files.isReadable(pathToParamFile)) {
            throw new ShiftPlanRunnerException(
                    "Der Dateipfad zur Parameterdatei (swap_params.json) '" + pathToParamFile +"' ist ungültig");
        }
        try {
            return mapper.readValue(pathToParamFile.toFile(), SwapParams.class);
        } catch (IOException ex) {
            logger.error("Ausnahme beim Zugriff auf Parameterdatei (swap_params.json)", ex);
            throw new ShiftPlanRunnerException(ex.getMessage());
        }
    }

    public static SwapParams readFromString(String paramString) {
        // Parameter müssen in folgender Reihenfolge, getrennt durch Komma, angegeben werden:
        // "SWAP|REPLACE,true|false,emp1-ID,cwIndex1,emp2-ID,(cwIndex2)"
        // Modus CREATE: Anzahl Parameter = 1
        // Modus SWAP: Anzahl Parameter = 6
        // Modus REPLACE: Anzahl Parameter = 5
        Objects.requireNonNull(paramString, "Keine Parameter vorhanden!");
        paramString = paramString.strip();

        SwapParams swapParams = new SwapParams();

        String[] paramArray = paramString.split("\\s*,\\s*");
        String modeStr = paramArray.length > 0 ? paramArray[0].toUpperCase() : null;
        if (modeStr == null) {
            throw new ShiftPlanSwapException("Ungültige Parameter-Übergabe!");
        }
        switch (modeStr) {
            case "CREATE" -> {
                swapParams.setMode(OP_MODE.valueOf(modeStr));
                // Weitere Parameter aus swap_params.json werden für die Erstellung eines neuen Schichtplans nicht
                // benötigt, SwapParams-Objekt daher an dieser Stelle schon zurückgeben
                return swapParams;
            }
            case "SWAP" -> Objects.checkIndex(5, paramArray.length);
            case "REPLACE" -> Objects.checkIndex(4, paramArray.length);
            default -> throw new ShiftPlanSwapException("Ungültige Parameter-Übergabe!");
        }

        swapParams.setMode(OP_MODE.valueOf(modeStr));
        IntStream.range(1, paramArray.length).forEach(index -> {
            switch (index) {
                case 1:
                    swapParams.setSwapHo(Boolean.parseBoolean(paramArray[index]));
                    break;
                case 2:
                    String emp1ID = paramArray[index];
                    if (emp1ID.matches("ID-\\d{1,2}")) {
                        swapParams.setEmployeeSet1(new String[]{emp1ID, paramArray[index + 1]});
                    }
                    break;
                case 4:
                    String emp2ID = paramArray[index];
                    if (emp2ID.matches("ID-\\d{1,2}"))  {
                        if (swapParams.getMode() == OP_MODE.SWAP) {
                            swapParams.setEmployeeSet2(new String[]{emp2ID, paramArray[index + 1]});
                        } else {
                            swapParams.setEmployeeSet2(new String[]{emp2ID});
                        }
                    }
                    break;
                default:
                    if (index >= paramArray.length) {
                        throw new ShiftPlanSwapException("Ungültige Anzahl von Parametern!");
                    }
            }
        });

        return swapParams;
    }


    public SwapParams() {}

    public OP_MODE getMode() {
        return mode;
    }

    public void setMode(OP_MODE mode) {
        this.mode = mode;
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
