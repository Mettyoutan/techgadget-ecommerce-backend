package com.techgadget.ecommerce.domain;

public enum OrderStatus {
    PENDING, // Waiting for payment
    CONFIRMED, // Payment success, processing the order
    SHIPPED, // Order is shipped
    COMPLETED, // Order already confirmed by customer
    FAILED, // Failed because of payment
    CANCELLED; // Cancelled by customer or admin

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == COMPLETED || target == CANCELLED;
            case FAILED, COMPLETED, CANCELLED -> false;
        };
    }
}
