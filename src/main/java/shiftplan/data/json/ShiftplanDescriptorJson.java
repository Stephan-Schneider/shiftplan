package shiftplan.data.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.ShiftPlanRunnerException;
import shiftplan.calendar.ShiftPolicy;
import shiftplan.data.IShiftplanDescriptor;
import shiftplan.users.Employee;
import shiftplan.web.ConfigBundle;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class ShiftplanDescriptorJson implements IShiftplanDescriptor {

    private static final Logger logger = LogManager.getLogger(ShiftplanDescriptorJson.class);
    private static ObjectMapper mapper;

    private static ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        }
        return mapper;
    }

    public static ShiftplanDescriptorJson readObject(String... jsonData) {
        Reader in;
        if (jsonData.length == 0) {
            ConfigBundle bundle = ConfigBundle.INSTANCE;
            String jsonPath = bundle.getJsonFile();
            if (jsonPath == null || jsonPath.isEmpty()) {
                logger.warn("Kein Json-Pfad konfiguriert!");
                throw new ShiftPlanRunnerException("Kein Pfad zur Json-Konfigurationsdatei hinterlegt");
            }

            File jsonFile = new File(jsonPath);
            try {
                in = new BufferedReader(new FileReader(jsonFile));
            } catch (FileNotFoundException fne) {
                logger.warn("Json-Datei {} existiert nicht", jsonFile.getAbsolutePath());
                throw new ShiftPlanRunnerException(("Json-Datei existiert nicht"));
            }

        } else {
            in = new BufferedReader(new StringReader(jsonData[0]));
        }

        ObjectMapper objectMapper = ShiftplanDescriptorJson.getMapper();
        try {
            ShiftplanDescriptorJson descriptor = objectMapper.readValue(in, ShiftplanDescriptorJson.class);
            descriptor.createPolicy();
            return descriptor;
        } catch (IOException e) {
            logger.error("Fehler beim Parsen der Json-Datei", e);
            throw new ShiftPlanRunnerException(e.getMessage());
        }
    }

    private Validity validity;
    private Policy policy;
    private Employees employees;

    public ShiftplanDescriptorJson() {
        super();
    }

    public Validity getValidity() {
        return validity;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public void setEmployees(Employees employees) {
        this.employees = employees;
    }

    @Override
    public int getYear() {
        return validity.getYear();
    }

    @Override
    public LocalDate getStartDate() {
       int month = validity.getStartDate();
       int year = validity.getYear();
       return LocalDate.of(year, month, 1);

    }

    @Override
    public LocalDate getEndDate() {
        int month = validity.getEndDate();
        int year = validity.getYear();
        LocalDate tmp = LocalDate.of(year, month, 1);
        return LocalDate.of(tmp.getYear(), tmp.getMonth(), tmp.lengthOfMonth());
    }

    @Override
    public List<LocalDate> getHolidays() {
        return validity
                .getHolidays()
                .stream()
                .map(Validity.Holiday::getDate)
                .toList();
    }

    @Override
    public Employee[] getEmployees() {
        List<Employee> employeeList = new ArrayList<>();
        Map<Employee, List<String>> tmpEmployeeMap = new HashMap<>();
        employees.getEmployees().forEach(pojo -> {
            Employee emp = new Employee(
                    pojo.getId(),
                    pojo.getName(),
                    pojo.getLastname(),
                    Employee.PARTICIPATION_SCHEMA.valueOf(pojo.getParticipation()),
                    pojo.getColor(),
                    pojo.getEmail()
            );
            tmpEmployeeMap.put(emp, Arrays.asList(pojo.getBackups()));
            employeeList.add(emp);
        });

       IShiftplanDescriptor.addBackups(employeeList, tmpEmployeeMap, logger);
       return employeeList.toArray(new Employee[0]);
    }

    private void createPolicy() {
        ShiftPolicy shiftPolicy = ShiftPolicy.INSTANCE;
        ShiftPolicy.Builder builder = new ShiftPolicy.Builder();
        builder.setLateShiftPeriod(policy.getLateShiftPeriod());
        builder.setMaxHoDaysPerMonth(policy.getMaxHomePerMonth());
        builder.setWeeklyHoCreditsPerEmployee(policy.getHoCreditsPerEmployee());
        builder.setMaxHoSlots(policy.getMaxHoSlotsPerDay());
        builder.setMaxSuccessiveHODays(policy.getMaxSuccessiveHoDays());
        builder.setMinDistanceBetweenHOBlocks(policy.getMinDistanceBetweenHoBlocks());
        for (String weekday: policy.getNoLateShiftOn()) {
            builder.addNoLateShiftOn(weekday);
        }

        shiftPolicy.createShiftPolicy(builder);
    }
}
