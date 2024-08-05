package shiftplan.data.json;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Employees {

    @JsonSetter("employeeList")
    private List<EmployeePojo> employees;


    public List<EmployeePojo> getEmployees() {
        return employees;
    }

    public void setEmployees(List<EmployeePojo> employees) {
        this.employees = employees;
    }

    static class EmployeePojo {

        private String id;
        private String name;
        @JsonSetter("lastName")
        private String lastname;
        private String participation;
        private String email;
        private String color;
        private String[] backups;

        public EmployeePojo() {
            super();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getParticipation() {
            return participation;
        }

        public void setParticipation(String participation) {
            this.participation = participation;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String[] getBackups() {
            return backups;
        }

        public void setBackups(String[] backups) {
            this.backups = backups;
        }

        @Override
        public String toString() {
            return "EmployeePojo{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", lastname='" + lastname + '\'' +
                    ", participation='" + participation + '\'' +
                    ", email='" + email + '\'' +
                    ", color='" + color + '\'' +
                    ", backups=" + Arrays.toString(backups) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EmployeePojo that = (EmployeePojo) o;
            return getId().equals(that.getId())
                    && getName().equals(that.getName())
                    && getLastname().equals(that.getLastname())
                    && getParticipation().equals(that.getParticipation())
                    && Objects.equals(getEmail(), that.getEmail()) &&
                    Objects.equals(getColor(), that.getColor());
        }

        @Override
        public int hashCode() {
            int result = getId().hashCode();
            result = 31 * result + getName().hashCode();
            result = 31 * result + getLastname().hashCode();
            result = 31 * result + getParticipation().hashCode();
            result = 31 * result + Objects.hashCode(getEmail());
            result = 31 * result + Objects.hashCode(getColor());
            return result;
        }
    }
}
