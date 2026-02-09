package com.techgadget.ecommerce.entity;

import com.techgadget.ecommerce.domain.OrderStatus;
import com.techgadget.ecommerce.domain.PaymentMethod;
import com.techgadget.ecommerce.domain.PaymentStatus;
import com.techgadget.ecommerce.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One user has many order
 *
 */
@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_number", columnNames = {"order_number"})
})
@Getter
@Setter
@NoArgsConstructor
public class Order extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Order number format: ORD-<timestamp>-<sequence>
     * Example: ORD-1704067200000-0001
     */
    @Column(nullable = false, length = 50)
    private String orderNumber;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address shippingAddress;

    /**
     * Payment status is always pending when created
     * Till payment happen
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    public Order(User user, String orderNumber, Address shippingAddress, String paymentMethod) {
        this.user = user;
        this.orderNumber = orderNumber;
        this.shippingAddress = shippingAddress;
        try {
            this.paymentMethod = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid PaymentMethod.");
        }

    }

    public void addItem(OrderItem orderItem) {
        orderItem.setOrder(this);
        items.add(orderItem);
    }

    /**
     * Get total price from each item
     */
    public Long getTotalPrice() {
        return this.items.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }

    public Integer getTotalItems() {
        return this.items.size();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
