package mdl.order_system_test.model;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_AUTHORIZED,
    FRAUD_CLEARED,
    COMPLETED,
    FAILED,
    COMPENSATED
}
