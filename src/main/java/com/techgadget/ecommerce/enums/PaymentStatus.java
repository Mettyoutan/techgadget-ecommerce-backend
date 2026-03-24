package com.techgadget.ecommerce.enums;

public enum PaymentStatus {
    PENDING, // Waiting for payment
    PAID,
    FAILED,
    REFUNDED // cancelled after paid, so payment was refunded
}
