package shiftplan.data.json;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;

public class Policy {

    @JsonSetter("lateshiftPeriod")
    private int lateShiftPeriod;
    @JsonSetter("maxHoDaysPerMonth")
    private int maxHomePerMonth;
    @JsonSetter("weeklyHoCreditsPerEmployee")
    private int hoCreditsPerEmployee;
    private int maxHoSlotsPerDay;
    private int maxSuccessiveHoDays;
    private int minDistanceBetweenHoBlocks;
    @JsonSetter("noLateshiftOn")
    private String[] noLateShiftOn;

    public Policy() {
        super();
    }

    public int getLateShiftPeriod() {
        return lateShiftPeriod;
    }

    public void setLateShiftPeriod(int lateShiftPeriod) {
        this.lateShiftPeriod = lateShiftPeriod;
    }

    public int getMaxHomePerMonth() {
        return maxHomePerMonth;
    }

    public void setMaxHomePerMonth(int maxHomePerMonth) {
        this.maxHomePerMonth = maxHomePerMonth;
    }

    public int getHoCreditsPerEmployee() {
        return hoCreditsPerEmployee;
    }

    public void setHoCreditsPerEmployee(int hoCreditsPerEmployee) {
        this.hoCreditsPerEmployee = hoCreditsPerEmployee;
    }

    public int getMaxHoSlotsPerDay() {
        return maxHoSlotsPerDay;
    }

    public void setMaxHoSlotsPerDay(int maxHoSlotsPerDay) {
        this.maxHoSlotsPerDay = maxHoSlotsPerDay;
    }

    public int getMaxSuccessiveHoDays() {
        return maxSuccessiveHoDays;
    }

    public void setMaxSuccessiveHoDays(int maxSuccessiveHoDays) {
        this.maxSuccessiveHoDays = maxSuccessiveHoDays;
    }

    public int getMinDistanceBetweenHoBlocks() {
        return minDistanceBetweenHoBlocks;
    }

    public void setMinDistanceBetweenHoBlocks(int minDistanceBetweenHoBlocks) {
        this.minDistanceBetweenHoBlocks = minDistanceBetweenHoBlocks;
    }

    public String[] getNoLateShiftOn() {
        return noLateShiftOn;
    }

    public void setNoLateShiftOn(String[] noLateShiftOn) {
        this.noLateShiftOn = noLateShiftOn;
    }

    @Override
    public String toString() {
        return "Policy{" +
                "lateShiftPeriod=" + lateShiftPeriod +
                ", maxHomePerMonth=" + maxHomePerMonth +
                ", hoCreditsPerEmployee=" + hoCreditsPerEmployee +
                ", maxHoSlotsPerDay=" + maxHoSlotsPerDay +
                ", minDistanceBetweenHoBlocks=" + minDistanceBetweenHoBlocks +
                ", noLateShiftsOn=" + Arrays.toString(noLateShiftOn) +
                '}';
    }
}
