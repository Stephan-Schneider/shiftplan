package shiftplan.calendar;

import shiftplan.users.Employee;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ShiftSwapDataModelConverter {

    private final ShiftPlanCopy copy;

    public ShiftSwapDataModelConverter(ShiftPlanCopy copy) {
        this.copy = Objects.requireNonNull(copy);
    }

    public static Map<String, Integer> getShiftInfo()  {
        ShiftPolicy policy = ShiftPolicy.INSTANCE;

        Map<String, Integer> shiftInfo = new HashMap<>();
        shiftInfo.put("hoSlotsPerShift", policy.getMaxHoSlots());
        shiftInfo.put("hoCreditsPerWeek", policy.getWeeklyHoCreditsPerEmployee());
        shiftInfo.put("maxHoDaysPerMonth", policy.getMaxHoDaysPerMonth());
        shiftInfo.put("lateShiftDuration", policy.getLateShiftPeriod());
        shiftInfo.put("maxSuccessiveHODays", policy.getMaxSuccessiveHODays());
        shiftInfo.put("minDistanceBetweenHOBlocks", policy.getMinDistanceBetweenHOBlocks());
        return shiftInfo;
    }

    public int getYear() {
        return copy.getForYear();
    }

    public LocalDate getStartDate() {
        return copy.getFrom();
    }

    public LocalDate getEndDate() {
        return copy.getTo();
    }

    public Employee[] getEmployees() {
        return copy.getEmployees();
    }

    public Map<String, Shift> getShiftPlan() {
        Map<String, Shift> shiftPlan = new HashMap<>();
        Map<Integer, ShiftPlanCopy.WorkDay[]> calendar = copy.getSimpleCalenderWeeks();
        calendar.values().forEach(workDays -> {
            for (ShiftPlanCopy.WorkDay workDay : workDays) {
                LocalDate date = workDay.getDate();
                Shift shift = new Shift(date);
                shift.setLateShift(workDay.getLateshift());
                workDay.getEmployeesInHo().forEach(shift::addEmployeeInHo);
                shiftPlan.put(date.toString(), shift);
            }
        });
        return shiftPlan;
    }

    public Map<Integer, LocalDate[]> getCalendar() {
        Map<Integer, LocalDate[]> calendar = new TreeMap<>(Integer::compareTo);
        copy.getSortedKeys().forEach( calendarWeek -> calendar.put(calendarWeek.cwIndex(), calendarWeek.cwDates()));
        return calendar;
    }
}
