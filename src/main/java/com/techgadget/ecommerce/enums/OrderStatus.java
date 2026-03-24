package com.techgadget.ecommerce.enums;

public enum OrderStatus {
    PENDING, // Waiting for payment
    PROCESSING, // order is processed by admin
    SHIPPED, // Order is shipped
    COMPLETED, // Order already confirmed by customer
    CANCELLED; // Cancelled by customer or admin

    public boolean canTransitionTo(OrderStatus target, UserRole role) {

        // Admin can cancel SHIPPED order
        // & process order transition
        if (role.equals(UserRole.ADMIN)) {
            return switch (this) {
                case PENDING -> target == PROCESSING || target == CANCELLED;
                case PROCESSING-> target == SHIPPED || target == CANCELLED;
                case SHIPPED -> target == COMPLETED || target == CANCELLED;
                case COMPLETED, CANCELLED -> false;
            };
        }

        // Customer can only cancel UNSHIPPED order
        return switch (this) {
            case PENDING, PROCESSING -> target == CANCELLED;
            default -> false;
        };
    }
}
