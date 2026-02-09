package com.techgadget.ecommerce.entity;

import com.techgadget.ecommerce.exception.ConflictException;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * One CartItem can be in many OrderItem
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price of single product at time of order (order price != product price)
     */
    @Column(nullable = false)
    private Long priceAtOrder;

    public OrderItem(Order order, Product product, Integer quantity, Long priceAtOrder) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    /**
     * Get subtotal = priceAtOrder * quantity
     */
    public Long getSubtotal() {
        return this.priceAtOrder * this.quantity;
    }

    /**
     * Set quantity (check if quantity sufficient with stock)
     * @throws ConflictException
     */
    public void setQuantity(int quantity) {
        if (this.product.isQuantitySufficient(quantity)) {
            this.quantity = quantity;
        } else {
            throw new ConflictException("Product stock is not sufficient for this quantity.");
        }
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
