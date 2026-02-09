package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.Cart;
import com.techgadget.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find cart by user ID
     * JOIN FETCH items and items.product
     */
    @Query("select distinct c from Cart c " +
            "left join fetch c.items ci " +
            "left join fetch ci.product " +
            "where c.user.id = :userId")
    Optional<Cart> findByUser_IdWithItems(@Param("userId") Long userId);

    /**
     * Find cart by user ID without items
     */
    Optional<Cart> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
