package com.techgadget.ecommerce.entity;

import com.techgadget.ecommerce.exception.ConflictException;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Using long because of RUPIAH currency
     */
    @Column(nullable = false)
    private Long priceInRupiah;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Setter(value = AccessLevel.NONE)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    /**
     * Product specifications using flexible JSONB
     * Example: {"ram": "8GB", "storage": "256GB", "processor": "A17"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> specs;

    public Product(Category category, String name, @Nullable String description, Long priceInRupiah, Integer stockQuantity, Map<String, Object> specs) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.priceInRupiah = priceInRupiah;
        this.stockQuantity = stockQuantity;
        this.specs = specs;
    }

    public void addImage(ProductImage image) {
        this.images.add(image);
    }

    public void removeImage(ProductImage image) {
        this.images.remove(image);
    }


    /**
     * Decrease stock
     */
    public void decreaseStockQuantity(int stockQuantity) {
        if (this.stockQuantity - stockQuantity < 0) {
            throw new ConflictException("Stock quantity not sufficient.");
        }
        this.stockQuantity = this.stockQuantity - stockQuantity;
    }

    /**
     * Check if stock quantity sufficient
     */
    public boolean isQuantitySufficient(int quantity) {
        return this.stockQuantity >= quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Product product)) return false;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
