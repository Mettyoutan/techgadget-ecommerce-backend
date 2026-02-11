package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.domain.OrderStatus;
import com.techgadget.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Get order by id with
     * - items
     * - payment
     * - products
     * - shipping address
     */
    @Query("select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where o.id = :orderId " +
            "and o.user.id = :userId")
    Optional<Order> findUserOrderByIdWithRelation(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );

    /**
     * Find all user orders with relation
     * -
     *  Filtering by:
     * - orderStatus
     * - fromDate
     * - toDate
     */
    @Query(
            value = "select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where o.user.id = :userId " +
            "and (:orderStatus is null or o.orderStatus = :orderStatus) " +
            "and o.createdAt >= :fromDate " +
            "and o.createdAt <= :toDate ",
            countQuery = """
                select count(o) from Order o
                where o.user.id = :userId
                and (:orderStatus is null or o.orderStatus = :orderStatus)
                and o.createdAt >= :fromDate
                and o.createdAt <= :toDate
                """
    )
    Page<Order> findUserOrdersBetweenDateWithRelation(
            @Param("userId") Long userId,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    /**
     * Find all user orders with relation (items and products)
     * -
     *  Filtering by:
     * - orderStatus
     * WITHOUT any date range
     */
    @Query(
            value = "select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where o.user.id = :userId " +
            "and (:orderStatus is null or o.orderStatus = :orderStatus)",
            countQuery = """
                select count(o) from Order o
                where o.user.id = :userId
                and (:orderStatus is null or o.orderStatus = :orderStatus)
                """
    )
    Page<Order> findUserOrdersWithRelation(
            @Param("userId") Long userId,
            @Param("orderStatus") OrderStatus orderStatus,
            Pageable pageable
    );

    /**
     * Find all user orders with relation (items and products)
     * -
     *  Filtering by:
     * - orderStatus
     * - fromDate
     */
    @Query(
            value = "select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where o.user.id = :userId " +
            "and (:orderStatus is null or o.orderStatus = :orderStatus) " +
            "and o.createdAt >= :fromDate ",
            countQuery = """
                select count(o) from Order o
                where o.user.id = :userId
                and (:orderStatus is null or o.orderStatus = :orderStatus)
                and o.createdAt >= :fromDate
                """
    )
    Page<Order> findUserOrdersFromDateWithRelation(
            @Param("userId") Long userId,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("fromDate") LocalDateTime fromDate,
            Pageable pageable
    );

    /**
     * Find all user orders with relation (items and products)
     * -
     *  Filtering by:
     * - orderStatus
     * - toDate
     */
    @Query(
            value = "select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where o.user.id = :userId " +
            "and (:orderStatus is null or o.orderStatus = :orderStatus) " +
            "and o.createdAt <= :toDate ",
            countQuery = """
                select count(o) from Order o
                where o.user.id = :userId
                and (:orderStatus is null or o.orderStatus = :orderStatus)
                and o.createdAt <= :toDate
                """
    )
    Page<Order> findUserOrdersToDateWithRelation(
            @Param("userId") Long userId,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where o.orderNumber = :orderNumber " +
            "and o.user.id = :userId")
    Optional<Order> findUserOrderByOrderNumberWithRelation(
            @Param("orderNumber") String orderNumber,
            @Param("userId") Long userId
    );

    boolean existsByIdAndUser_Id(Long id, Long userId);

    // Count all user orders
    long countByUser_Id(Long userId);


    @Query(
            value = "select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where (:orderStatus is null or o.orderStatus = :orderStatus)",
            countQuery = """
                select count(o) from Order o
                where (:orderStatus is null or o.orderStatus = :orderStatus)
                """
    )
    Page<Order> findAllOrdersWithRelation(
            @Param("orderStatus") OrderStatus orderStatus,
            Pageable pageable
    );

    @Query(
            value = "select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where (:orderStatus is null or o.orderStatus = :orderStatus) " +
            "and o.createdAt >= :fromDate " +
            "and o.createdAt <= :toDate ",
            countQuery = """
                select count(o) from Order o
                where (:orderStatus is null or o.orderStatus = :orderStatus)
                and o.createdAt >= :fromDate
                and o.createdAt <= :toDate
                """
    )
    Page<Order> findAllOrdersBetweenDateWithRelation(
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query(
            value = "select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where (:orderStatus is null or o.orderStatus = :orderStatus) " +
            "and o.createdAt >= :fromDate ",
            countQuery = """
                select count(o) from Order o
                where (:orderStatus is null or o.orderStatus = :orderStatus)
                and o.createdAt >= :fromDate
                """
    )
    Page<Order> findAllOrdersFromDateWithRelation(
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("fromDate") LocalDateTime fromDate,
            Pageable pageable
    );

    @Query(
            value = "select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where (:orderStatus is null or o.orderStatus = :orderStatus) " +
            "and o.createdAt <= :toDate ",
            countQuery = """
                select count(o) from Order o
                where (:orderStatus is null or o.orderStatus = :orderStatus)
                and o.createdAt <= :toDate
                """
    )
    Page<Order> findAllOrdersToDateWithRelation(
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("select o from Order o " +
            "left join fetch o.shippingAddress " +
            "left join fetch o.payment " +
            "left join fetch o.items oi " +
            "left join fetch oi.product " +
            "where o.id = :orderId")
    Optional<Order> findOrderByIdWithRelationForAdmin(@Param("orderId") Long orderId);
}
