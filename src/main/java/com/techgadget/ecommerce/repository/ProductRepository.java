package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find paginated products by category id
     */
    Page<Product> findByCategory_Id(Long categoryId, Pageable pageable);

    /**
     * Find paginated products by name (case-insensitive)
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find paginated products within price range
     */
    Page<Product> findByPriceInRupiahBetween(
            Long minPrice, Long maxPrice, Pageable pageable);
    /**
     * Search paginated products
     * by name and category id (complex query)
     */
    @Query(
            value = "select p from Product p where " +
            "lower(p.name) like lower(concat('%', :name, '%') ) and " +
            "p.category.id = :categoryId",
            countQuery = """
                select count(p) from Product p
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.category.id = :categoryId
            """
    )
    Page<Product> searchByNameAndCategory_Id(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    /**
     * Search paginated products
     * by name, category id, and price range
     */
    @Query(
            value = "select p from Product p where " +
            "lower(p.name) like lower(concat('%', :name, '%') ) and " +
            "p.category.id = :categoryId and " +
            "p.priceInRupiah between :minPrice and :maxPrice",
            countQuery = """
                select count(p) from Product p
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.category.id = :categoryId
                and p.priceInRupiah between :minPrice and :maxPrice
            """
    )
    Page<Product> searchByNameAndCategory_IdAndPrice(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );

    /**
     * Search paginated products
     * by name and price range
     */
    @Query(
            value = "select p from Product p where " +
            "lower(p.name) like lower(concat('%', :name, '%') ) and " +
            "p.priceInRupiah between :minPrice and :maxPrice",
            countQuery = """
                select count(p) from Product p
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.priceInRupiah between :minPrice and :maxPrice
            """
    )
    Page<Product> searchByNameAndPrice(
            @Param("name") String name,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );

    /**
     * Find paginated products by stock availability
     */
    @Query(
            value = "select p from Product p where p.stockQuantity > 0",
            countQuery = "select count(p) from Product p where p.stockQuantity > 0"
    )
    Page<Product> findAvailableProducts(Pageable pageable);

    /**
     * Check if category exists
     */
    boolean existsByCategory_Id(Long categoryId);

}
