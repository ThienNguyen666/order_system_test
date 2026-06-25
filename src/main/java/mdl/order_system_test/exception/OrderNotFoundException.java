package mdl.order_system_test.exception;

public class OrderNotFoundException extends RuntimeException {
      public OrderNotFoundException(String message) {
            super(message);
      }
}