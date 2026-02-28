package com.techgadget.ecommerce.entity;

import com.techgadget.ecommerce.dto.response.image.StoredImageDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "product_images")
@NoArgsConstructor
@Getter
@Setter
public class ProductImage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String objectKey;

    private String thumbnailKey;

    private boolean isPrimary;

    public ProductImage(Product product, String objectKey, String thumbnailKey, boolean isPrimary) {
        this.product = product;
        this.objectKey = objectKey;
        this.thumbnailKey = thumbnailKey;
        this.isPrimary = isPrimary;
    }

    public static ProductImage create(Product product, StoredImageDto stored, boolean isPrimary) {
        return new ProductImage(
                product,
                stored.objectKey(),
                stored.thumbnailKey(),
                isPrimary
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProductImage that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
