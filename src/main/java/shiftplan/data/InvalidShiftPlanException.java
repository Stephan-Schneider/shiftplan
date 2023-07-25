package shiftplan.data;

public class InvalidShiftPlanException extends RuntimeException {

    public InvalidShiftPlanException(String errorMessage) {
        super(errorMessage);
    }
}
