package com.techgadget.ecommerce.entity;

import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends Auditable {

    @Setter(AccessLevel.NONE)
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
     * Price of each product
     * -
     * Using long because of RUPIAH currency
     */
    @Column(nullable = false)
    private Long price;

    /**
     * Total stock quantity
     */
    @Column(nullable = false)
    private Integer stock;

    @Setter(value = AccessLevel.NONE)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductImage> images = new ArrayList<>();

    /**
     * Product specifications using flexible JSONB
     * Example: {"ram": "8GB", "storage": "256GB", "processor": "A17"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> specs;

    public Product(Category category, String name, @Nullable String description, Long price, Integer stock, Map<String, Object> specs) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.specs = specs;
    }

    public void addImage(ProductImage image) {
        this.images.add(image);
    }

    public void removeImage(ProductImage image) {
        this.images.remove(image);
    }

    /**
     * Get a primary image key of product
     */
    public String getPrimaryImageKey() {
        ProductImage primary =  this.images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .orElse(null);

        if (primary == null) return null;

        return primary.getThumbnailKey() != null
                ? primary.getThumbnailKey()
                : primary.getOriginalKey();
    }

    /**
     * Get all image keys of product
     */
    public List<String> getAllImageKey() {
        return this.images.stream()
                .map(i ->
                        i.getThumbnailKey() != null
                            ? i.getThumbnailKey()
                            : i.getOriginalKey()
                ).toList();
    }

    /**
     * Decrease stock
     */
    public void decreaseStock(int stock) {
        if (this.stock - stock < 0) {
            throw new ConflictException("Stock quantity not sufficient.");
        }
        this.stock = this.stock - stock;
    }

    /**
     * Check if stock quantity sufficient
     */
    public boolean isStockSufficient(int quantity) {
        return this.stock >= quantity;
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
