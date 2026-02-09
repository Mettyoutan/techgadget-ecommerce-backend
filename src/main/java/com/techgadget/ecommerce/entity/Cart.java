package com.techgadget.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "carts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_id", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Cart extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * one user has one cart
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "cart", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    /**
     * Constructor to create new cart without any item
     */
    public Cart(User user) {
        this.user = user;
    }

    /**
     * Get total price of items in cart
     */
    public Long getTotalPrice() {
        // recalculate total price
        return this.items
                .stream()
                .mapToLong(item -> item.getProduct().getPriceInRupiah() * item.getQuantity())
                .sum();


    }

    /**
     * Get total items in cart
     */
    public Integer getTotalItems() {
        // recalculate total items
        return this.items
                .stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Add cart item
     */
    public void addItem(CartItem cartItem) {
        this.items.add(cartItem);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Cart cart)) return false;
        return Objects.equals(id, cart.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
