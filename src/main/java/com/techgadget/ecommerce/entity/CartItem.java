package com.techgadget.ecommerce.entity;

import com.techgadget.ecommerce.exception.ConflictException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        // Composite unique constraint (cart_id, product_id)
        @UniqueConstraint(name = "uk_cart_product", columnNames = {"cart_id", "product_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class CartItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private Integer quantity;

    public CartItem(Cart cart, Product product, Integer quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    /**
     * Get subtotal
     */
    public Long getSubTotal() {
        // recalculate sub total
        return this.product.getPriceInRupiah() * this.quantity;
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
        if (!(o instanceof CartItem cartItem)) return false;
        return Objects.equals(id, cartItem.id) && Objects.equals(product, cartItem.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, product);
    }
}
