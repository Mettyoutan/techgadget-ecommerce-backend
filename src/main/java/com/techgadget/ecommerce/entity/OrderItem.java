package com.techgadget.ecommerce.entity;

import com.techgadget.ecommerce.exception.ConflictException;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends Auditable {

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Product snapshot fields
     */
    @Column(nullable = false)
    private Long productIdSnapshot;

    @Column(nullable = false)
    private String productNameSnapshot;

    @Nullable
    private String productImageKeySnapshot;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price/item at time of order (order price != product price)
     */
    @Column(nullable = false)
    private Long priceAtOrder;

    @Column(nullable = false)
    private boolean reviewed = false;

    public OrderItem(
            Order order,
            Long productIdSnapshot,
            String productNameSnapshot,
            @Nullable String productImageKeySnapshot,
            Integer quantity,
            Long priceAtOrder
    ) {
        this.order = order;
        this.productIdSnapshot = productIdSnapshot;
        this.productNameSnapshot = productNameSnapshot;
        this.productImageKeySnapshot = productImageKeySnapshot;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    /**
     * Get subtotal = priceAtOrder * quantity
     */
    public Long getSubtotal() {
        return this.priceAtOrder * this.quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrderItem orderItem)) return false;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
