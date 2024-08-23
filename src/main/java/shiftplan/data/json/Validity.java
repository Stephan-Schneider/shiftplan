package shiftplan.data.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.LocalDate;
import java.util.List;

public class Validity {

    private int year;
    private int startDate;
    private int endDate;
    private boolean boundaryStrict;
    @JsonSetter("publicHolidays")
    private List<Holiday> holidays;

    public Validity() {
        super();
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getStartDate() {
        return startDate;
    }

    public void setStartDate(int startDate) {
        this.startDate = startDate;
    }

    public int getEndDate() {
        return endDate;
    }

    public void setEndDate(int endDate) {
        this.endDate = endDate;
    }

    public boolean isBoundaryStrict() {
        return boundaryStrict;
    }

    public void setBoundaryStrict(boolean boundaryStrict) {
        this.boundaryStrict = boundaryStrict;
    }

    public List<Holiday> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<Holiday> holidays) {
        this.holidays = holidays;
    }

    @Override
    public String toString() {
        return "Validity{" +
                "year=" + year +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", holidays=" + holidays +
                '}';
    }

    static class Holiday {
        private String name;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }
}
