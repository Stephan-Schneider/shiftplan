package shiftplan.users;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Employee implements Comparable<Employee> {

    private static final Logger logger = LogManager.getLogger(Employee.class);

    /*
    HO: Nur Homeoffice
    LS: Nur Spätschicht
    HO_LS: Homeoffice und Spätschicht
     */
    public enum PARTICIPATION_SCHEMA {HO, LS, HO_LS}

    private final String id;
    private final String name;
    private final String lastName;
    private String highlightColor;
    private String email;
    private final PARTICIPATION_SCHEMA schema;

    // Summe der innerhalb eines Monats zugewiesenen HO-Tage - darf maxHoDaysPerMonth nicht überschreiten
    private final Map<Integer, Integer> hoDaysTotalPerMonth = new TreeMap<>();

    // Summe der gesamten nicht-zugewiesenen HO-Tage - wird am Anfang jeder Kalenderwoche aus den Daten der Vorwoche aktualisiert
    // und dient der Priorisierung von HO-Kandidaten: je größer die Anzahl nicht zugewiesener HO-Tage, desto höher ist die
    // Priorität bei der Vergabe von HO-Slots
    private int notAssignedHoDays = 0;

    // Die Anzahl der HO-Tage pro Woche. Am Anfang einer Kalenderwoche wird die maximale Anzahl von HO-Tagen gesetzt,
    // bei jeder Vergabe von HO-Tagen wird das HO-Konto des MA's um 1 reduziert
    private int homeOfficeBalance;

    // Liste der Backups - diese Liste konstituiert eine Restriktion bei der Vergabe von HO-Tagen: Backups und dieser
    // MA schließen sich bei der HO-Vergabe an einem bestimmten Datum gegenseitig aus
    private final List<Employee> backups = new ArrayList<>();

    public Employee(String anId, String aName, String aLastName, PARTICIPATION_SCHEMA schema) {
        id = anId;
        name = aName;
        lastName = aLastName;
        this.schema = schema;

    }

    public Employee(String id, String name, String lastName, PARTICIPATION_SCHEMA schema, String highlightColor) {
        this(id, name, lastName, schema);
        this.highlightColor = highlightColor;
    }

    public Employee(String id, String name, String lastName, PARTICIPATION_SCHEMA schema, String highlightColor, String email) {
        this(id, name, lastName, schema, highlightColor);
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public PARTICIPATION_SCHEMA getParticipationSchema() {
        return schema;
    }

    public String getHighlightColor() {
        if (highlightColor == null || highlightColor.isEmpty()) {
            return "#000000";
        }
        return highlightColor;
    }

    public String getEmail() {
        return email;
    }

    public void addBackup(Employee backup) {
        assert backup != null && !this.equals(backup);
        backups.add(backup);
    }

    public void addBackups(List<Employee> backups) {
        if (backups != null && !backups.isEmpty()) {
            backups.forEach(this::addBackup);
        }
    }

    public List<Employee> getBackups() {
        return backups;
    }

    public void setHomeOfficeBalance(int homeOfficeBalance) {
        if (schema == PARTICIPATION_SCHEMA.HO || schema == PARTICIPATION_SCHEMA.HO_LS) {
            this.homeOfficeBalance = homeOfficeBalance;
        }
    }

    public int getHomeOfficeBalance() {
        return homeOfficeBalance;
    }

    public void countDownHomeOfficeBalance() {
        --homeOfficeBalance;
    }

    public boolean hasDaysLeft() {
        return homeOfficeBalance > 0;
    }

    public void swapBalance() {
        notAssignedHoDays += homeOfficeBalance;
        homeOfficeBalance = 0;
        logger.debug("{}: Nicht zugewiesene HO-Tage: {}", getName(), notAssignedHoDays);
    }

    public int getHoDaysTotalPerMonth(int month) {
        return hoDaysTotalPerMonth.getOrDefault(month, 0);
    }

    public void addToHoDaysTotal(int month) {
        hoDaysTotalPerMonth.merge(month, 1, Integer::sum);
    }

    public int getNotAssignedHoDays() {
        return notAssignedHoDays;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Employee{");
        sb.append("name='").append(name).append('\'');
        sb.append(", lastName='").append(lastName).append('\'');
        sb.append(", homeOfficeBalance='").append(homeOfficeBalance).append('\'');
        sb.append(", participationSchema=").append(getParticipationSchema().name());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Employee employee = (Employee) o;

        if (getParticipationSchema() != employee.getParticipationSchema()) return false;
        if (!getName().equals(employee.getName())) return false;
        if (!getLastName().equals(employee.getLastName())) return false;
        return (!getHighlightColor().equals(employee.getHighlightColor()));
    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getLastName().hashCode();
        result = 31 * result + getParticipationSchema().name().hashCode();
        result = 31 * result + getHighlightColor().hashCode();
        return result;
    }

    @Override
    public int compareTo(Employee other) {
        return Integer.compare(getNotAssignedHoDays(), other.getNotAssignedHoDays());
    }
}
