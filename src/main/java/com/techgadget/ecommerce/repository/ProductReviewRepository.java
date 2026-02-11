package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview,Integer> {

    @Query(
            value = """
                select pr from ProductReview pr
                left join fetch pr.user u
                left join fetch pr.product
                where pr.product.id = :productId
            """,
            countQuery = """
                select count(pr) from ProductReview pr
                where pr.product.id = :productId
            """

    )
    Page<ProductReview> findByProduct_IdWithRelation(
            @Param("productId") Long productId,
            Pageable pageable
    );

    // Check if user already review the product or not
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);
}
