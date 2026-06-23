package mdl.order_system_test.worker;

public class SimulatedTaskTimeoutException extends RuntimeException {

    public SimulatedTaskTimeoutException(String message) {
        super(message);
    }
}
