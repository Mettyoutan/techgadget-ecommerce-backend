package com.techgadget.ecommerce.domain;

public enum OrderStatus {
    PENDING, // Waiting for payment
    CONFIRMED, // Payment success, processing the order
    SHIPPED, // Order is shipped
    DELIVERED, // Order already delivered to address
    COMPLETED, // Order already confirmed by customer
    FAILED, // Failed because of payment
    CANCELLED, // Cancelled by customer or admin
}
