package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    Optional<ProductImage> findByIdAndProduct_Id(Long id, Long productId);

    Optional<ProductImage> findByProduct_IdAndIsPrimaryTrue(Long productId);
}
