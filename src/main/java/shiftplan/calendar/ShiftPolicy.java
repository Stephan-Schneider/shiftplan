package shiftplan.calendar;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class ShiftPolicy {

    public static final ShiftPolicy INSTANCE = new ShiftPolicy();

    private int lateShiftPeriod;
    private int maxHoSlots;
    private int weeklyHoCreditsPerEmployee;
    private int maxHoDaysPerMonth;
    private List<DayOfWeek> noLateShiftOn;

    private ShiftPolicy() {}

    public void createShiftPolicy(ShiftPolicy.Builder builder) {
        this.lateShiftPeriod = builder.lateShiftPeriod;
        this.maxHoSlots = builder.maxHoSlots;
        this.weeklyHoCreditsPerEmployee = builder.weeklyHoCreditsPerEmployee;
        this.maxHoDaysPerMonth = builder.maxHoDaysPerMonth;
        this.noLateShiftOn = builder.noLateShiftOn;
    }

    public int getLateShiftPeriod() {
        return lateShiftPeriod;
    }

    public int getMaxHoSlots() {
        return maxHoSlots;
    }

    public int getWeeklyHoCreditsPerEmployee() {
        return weeklyHoCreditsPerEmployee;
    }

    public int getMaxHoDaysPerMonth() {
        return maxHoDaysPerMonth;
    }

    public List<DayOfWeek> getNoLateShiftOn() {
        return noLateShiftOn;
    }

    public static class Builder {

        private int lateShiftPeriod;
        private int maxHoSlots;
        private int weeklyHoCreditsPerEmployee;
        private int maxHoDaysPerMonth;
        private final List<DayOfWeek> noLateShiftOn = new ArrayList<>();

        public void setLateShiftPeriod(int lateShiftPeriod) {
            this.lateShiftPeriod = lateShiftPeriod;
        }

        public void setMaxHoSlots(int maxHoSlots) {
            this.maxHoSlots = maxHoSlots;
        }

        public void setWeeklyHoCreditsPerEmployee(int weeklyHoCreditsPerEmployee) {
            this.weeklyHoCreditsPerEmployee = weeklyHoCreditsPerEmployee;
        }

        public void setMaxHoDaysPerMonth(int maxHoDaysPerMonth) {
            this.maxHoDaysPerMonth = maxHoDaysPerMonth;
        }

        public void addNoLateShiftOn(String weekday) {
            noLateShiftOn.add(DayOfWeek.valueOf(weekday.toUpperCase()));
        }
    }
}
