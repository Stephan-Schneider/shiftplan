package shiftplan.calendar;

public class ShiftPolicy {

    public static final ShiftPolicy INSTANCE = new ShiftPolicy();

    private int lateShiftPeriod;
    private int maxHoSlots;
    private int weeklyHoCreditsPerEmployee;
    private int maxHoDaysPerMonth;

    private ShiftPolicy() {}

    public void createShiftPolicy(ShiftPolicy.Builder builder) {
        this.lateShiftPeriod = builder.lateShiftPeriod;
        this.maxHoSlots = builder.maxHoSlots;
        this.weeklyHoCreditsPerEmployee = builder.weeklyHoCreditsPerEmployee;
        this.maxHoDaysPerMonth = builder.maxHoDaysPerMonth;
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

    public static class Builder {

        private int lateShiftPeriod;
        private int maxHoSlots;
        private int weeklyHoCreditsPerEmployee;
        private int maxHoDaysPerMonth;

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
    }
}
