package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find paginated products by name (case-insensitive)
     * Join:
     * > Primary image
     */
    @Query(
            value = """
                select p from Product p
                left join p.images i
                where lower(p.name) like lower(concat('%', :name, '%') )
                and i.isPrimary = true
            """,
            countQuery = """
                select count(p) from Product p
                left join p.images i
                where lower(p.name) like lower(concat('%', :name, '%') )
                and i.isPrimary = true
            """
    )
    Page<Product> findProductListByName(String name, Pageable pageable);

    /**
     * Search paginated products
     * by name and category id (complex query)
     * -
     * Join:
     * > Primary image
     */
    @Query(
            value = """
                select p from Product p
                join p.images i
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.category.id = :categoryId
                and i.isPrimary = true
            """,
            countQuery = """
                select count(p) from Product p
                join p.images i
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.category.id = :categoryId
                and i.isPrimary = true
            """
    )
    Page<Product> findProductListByNameAndCategory_Id(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    /**
     * Search paginated products
     * by name, category id, and price range
     * -
     * Join:
     * > Primary image
     */
    @Query(
            value = """
                select p from Product p
                left join p.images i
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.category.id = :categoryId
                and p.priceInRupiah between :minPrice and :maxPrice
                and i.isPrimary = true
            """,
            countQuery = """
                select count(p) from Product p
                left join p.images i
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.category.id = :categoryId
                and p.priceInRupiah between :minPrice and :maxPrice
                and i.isPrimary = true
            """
    )
    Page<Product> findProductListByNameAndCategory_IdAndPrice(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );

    /**
     * Search paginated products
     * by name and price range
     * -
     * Join:
     * > Primary image
     */
    @Query(
            value = """
                select p from Product p
                left join p.images i
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.priceInRupiah between :minPrice and :maxPrice
                and i.isPrimary = true
            """,
            countQuery = """
                select count(p) from Product p
                left join p.images i
                where lower(p.name) like lower(concat('%', :name, '%') )
                and p.priceInRupiah between :minPrice and :maxPrice
                and i.isPrimary = true
            """
    )
    Page<Product> findProductListByNameAndPrice(
            @Param("name") String name,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );

    /**
     * Find paginated products by stock availability
     * Join:
     * > Primary image
     */
    @Query(
            value = """
                select p from Product p
                left join p.images i
                where p.stockQuantity > 0
                and i.isPrimary = true
            """,
            countQuery = """
                select count(p) from Product p
                left join p.images i
                where p.stockQuantity > 0
                and i.isPrimary = true
            """
    )
    Page<Product> findAvailableProductList(Pageable pageable);

    /**
     * Check if category exists
     */
    boolean existsByCategory_Id(Long categoryId);

    @EntityGraph(attributePaths = {"images"})
    Optional<Product> findProductDetailById(Long id);

}
