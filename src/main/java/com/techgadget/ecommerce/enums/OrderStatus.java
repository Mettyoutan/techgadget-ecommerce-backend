package com.techgadget.ecommerce.enums;

public enum OrderStatus {
    PENDING, // Waiting for payment
    PAID, // Order is paid
    CONFIRMED, // Admin confirmed the paid order
    SHIPPED, // Order is shipped
    COMPLETED, // Order already confirmed by customer
    FAILED, // Failed because of payment
    CANCELLED; // Cancelled by customer or admin

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == PAID || target == FAILED;
            case PAID -> target == CONFIRMED || target == CANCELLED || target == FAILED;
            case CONFIRMED -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == COMPLETED || target == CANCELLED;
            case FAILED, COMPLETED, CANCELLED -> false;
        };
    }
}
