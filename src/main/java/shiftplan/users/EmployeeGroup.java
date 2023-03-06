package shiftplan.users;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class EmployeeGroup {

    private static final Logger logger = LogManager.getLogger(EmployeeGroup.class);

    private static final List<Employee> allEmployees = new ArrayList<>();

    private final String groupName;
    private final Employee[] employees;
    private final List<LocalDate> homeOfficePlan;
    private final Map<Integer, List<LocalDate>> homeOfficeDaysByMonth = new HashMap<>();

    public EmployeeGroup(String groupName, Employee[] employees) {
        this.groupName = groupName;
        this.employees = employees;
        allEmployees.addAll(Arrays.asList(employees));
        homeOfficePlan = new ArrayList<>();

        setReverseRelationShip();
    }

    private void setReverseRelationShip() {
        for (Employee e : employees) {
            e.setEmployeeGroup(this);
        }
    }

    public static List<Employee> getAllEmployees() {
        return allEmployees;
    }

    public static Employee[] getEmployeesInShiftOrder() {
        List<Employee> employeesForLateShift = new ArrayList<>(allEmployees.stream()
                // index >= 0: Mitarbeiter partizipiert an Spätschicht, index < 0: Mitarbeiter partizipiert nicht
                .filter(employee -> employee.getLateShiftOrder() >= 0)
                .toList());
        Collections.sort(employeesForLateShift);

       return employeesForLateShift.toArray(new Employee[] {});
    }

    public String getGroupName() {
        return groupName;
    }

    public Employee[] getEmployees() {
        return employees;
    }

    public List<LocalDate> getHomeOfficePlan() {
        return homeOfficePlan;
    }

    public void addToPlan(List<LocalDate> dates) {
        homeOfficePlan.addAll(dates);
        dates.forEach(homeOfficeDate -> {
            // Filtern der Homeoffice-Daten nach Monat
            homeOfficeDaysByMonth.computeIfAbsent(homeOfficeDate.getMonthValue(), k -> new ArrayList<>());
            List<LocalDate> datesByMonth = homeOfficeDaysByMonth.get(homeOfficeDate.getMonthValue());
            datesByMonth.add(homeOfficeDate);
        });
    }

    public Map<Integer, List<LocalDate>> getHomeOfficeDaysByMonth() {
        return homeOfficeDaysByMonth;
    }

    public List<HomeOfficeRecord> calculateHomeOfficeOptionByMonth(int year, int maxPerWeek, int maxPerMonth) {
        logger.info("Homeoffice-Zuweisungen gem. Plan werden evaluiert für Gruppe: {}", getGroupName());
        List<HomeOfficeRecord> records = new ArrayList<>();
        for (Integer month : homeOfficeDaysByMonth.keySet()) {
            // Monatsweise Kalkulation der verplanten und nicht zugewiesenen HO-Tage
            logger.trace("Daten für Gruppe {} im Monat {}.{}", getGroupName(), month, year);
            List<LocalDate> datesByMonth = homeOfficeDaysByMonth.get(month);
            // Gesamtanzahl der zugewiesenen HO-Tage im betreffenden Monat (dürfen nicht mehr als <maxPerMonth> sein)
            int totalOptionsPerMonth = 0;
            // Counter für die zugewiesenen HO-Tage in einer Woche (dürfen insgesamt nicht mehr als <maxPerWeek> pro Woche sein)
            int optionsPerWeekCounter = 0;
            LocalDate current = LocalDate.of(year, month, 1); // die Prüfung beginnt immer am 01. eines Monats
            int lengthOfMonth = current.lengthOfMonth();
            for (int i = 1; i <= lengthOfMonth; i++) {
                if (datesByMonth.contains(current)) {
                    logger.trace("OptionsPerWeekCounter wird hochgezählt für Datum {} / Aktueller Wert: {}",
                            current, optionsPerWeekCounter);
                    if (optionsPerWeekCounter < maxPerWeek) {
                        ++optionsPerWeekCounter;
                    }
                    logger.trace("OptionsPerWeekCounter nach Inkrement: {}", optionsPerWeekCounter);
                }
                if (current.getDayOfWeek() == DayOfWeek.SATURDAY || i == lengthOfMonth) {
                    logger.trace("Wochenende (Samstag) oder Ende des Monats erreicht!");
                    logger.trace("Wochenoptionen am Samstag, den {}: {}", current, optionsPerWeekCounter);
                    totalOptionsPerMonth += optionsPerWeekCounter;
                    logger.trace("Monats-HO-Optionen bis {}: {}", current, totalOptionsPerMonth);
                    if (totalOptionsPerMonth > maxPerMonth) {
                        totalOptionsPerMonth = maxPerMonth;
                        logger.trace("Monats-HO-Optionen nach Korrektur bis {}: {}", current, totalOptionsPerMonth);
                    }
                    optionsPerWeekCounter = 0; // Wochenzähler am Ende der Woche zurücksetzen
                }
                current = current.plusDays(1);
            }
            HomeOfficeRecord homeOfficeRecord = new HomeOfficeRecord(
                    getGroupName(), month, totalOptionsPerMonth, maxPerMonth - totalOptionsPerMonth);
            logger.info("Zugewiesene / Nicht zugewiesene HO-Tage für Gruppe {} in {}{}: {}",
                    getGroupName(), month, year, homeOfficeRecord);
            records.add(homeOfficeRecord);
        }
        return records;
    }
}
