package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select ci from CartItem ci " +
            "left join fetch ci.product " +
            "where ci.id = :id " +
            "and ci.cart.user.id = :userId")
    Optional<CartItem> findByIdAndCart_User_IdWithProduct(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    boolean deleteByIdAndCart_User_Id(Long id, Long cartUserId);

    /**
     * Find specify item
     * JOIN FETCH product
     */
    @Query("select ci from CartItem ci " +
            "left join fetch ci.product " +
            "where ci.cart.id = :cartId and ci.product.id = :productId")
    Optional<CartItem> findByCart_IdAndProduct_IdWithProduct(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId
    );

    boolean existsByCart_IdAndProduct_Id(Long cartId, Long productId);

    /**
     * Count total items by cart ID
     */
    long countByCart_Id(Long cartId);
}
