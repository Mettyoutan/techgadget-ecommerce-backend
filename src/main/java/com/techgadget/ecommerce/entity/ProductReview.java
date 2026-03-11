package com.techgadget.ecommerce.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(
        name = "product_reviews",
        uniqueConstraints = {
            // 1 order item = 1 review
            @UniqueConstraint(columnNames = {"user_id", "order_item_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductReview extends Auditable {

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reviewer
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * OrderItem reference to ensure user bought the product
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /**
     * Product being reviewed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer rating; // 1 - 5

    /**
     * Optional comment
     */
    @Column(length = 500)
    private String comment;

    public ProductReview(
            User user,
            OrderItem orderItem,
            Product product,
            Integer rating,
            @Nullable String comment
    ) {
        this.user = user;
        this.orderItem = orderItem;
        this.product = product;
        this.rating = rating;
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProductReview that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
