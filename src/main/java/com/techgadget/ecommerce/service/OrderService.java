package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.domain.OrderStatus;
import com.techgadget.ecommerce.domain.PaymentMethod;
import com.techgadget.ecommerce.domain.PaymentStatus;
import com.techgadget.ecommerce.dto.request.CreateOrderRequest;
import com.techgadget.ecommerce.dto.request.OrderFilterRequest;
import com.techgadget.ecommerce.dto.request.UpdateOrderStatusToShipRequest;
import com.techgadget.ecommerce.dto.response.AddressResponse;
import com.techgadget.ecommerce.dto.response.OrderResponse;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.entity.*;
import com.techgadget.ecommerce.exception.BadRequestException;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    // -------------------------
    // --- CUSTOMER METHODS ---
    // -------------------------

    /**
     * Create order using selected cart items, not all
     * Cart items are not getting removed
     * Product stock is decreased when order is created (status PENDING)
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {

        log.debug("Processing create order - " +
                        "User: {}, CartItem: {}, Address: {}, PaymentMethod: {}",
                userId,
                request.getCartItemIds(),
                request.getAddressId(),
                request.getPaymentMethod());

        // 0. Check if requested cart items > 0
        if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
            log.warn("Requested cart items is empty for order creation with user id = {}", userId);
            throw new BadRequestException("No cart items selected");
        }

        log.debug("0 success");

        // 1. Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id {}", userId);
                    return new NotFoundException("User not found");
                });

        log.debug("1 success");

        // 2. Find user cart
        Cart cart = cartRepository.findByUser_IdWithItems(userId)
                .orElseThrow(() -> {
                    log.warn("Cart not found with user id = {}", userId);
                    return new NotFoundException("Cart not found.");
                });

        log.debug("2 success");

        // 3. Make sure cart items not empty
        if (cart.getItems().isEmpty()) {
            log.warn("Cannot create order because of empty cart with id = {}", cart.getId());
            throw new ConflictException("Cart is empty.");
        }

        log.debug("3 success");

        // 4. Check if address exists and belongs to this user
        Address shippingAddress = addressRepository.findByIdAndUser_Id(request.getAddressId(), userId)
                .orElseThrow(() -> {
                    log.warn("Address not found with id = {} and user id = {}",
                            request.getAddressId(), userId);
                    return new NotFoundException("Address not found.");
                });

        log.debug("4 success");

        /*
            5. Filter wanted cart item
            6. Validate each product stock
            7. Subtract each product stock
            8. Create list of OrderItem
         */
        List<OrderItem> orderItems = new ArrayList<>();

        for (Long cartItemId : request.getCartItemIds()) {

            // 5
            CartItem cartItem = cart.getItems().stream()
                    .filter(ci -> ci.getId().equals(cartItemId))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.warn("Wanted cart item not found with id = {} for user id = {}",
                                cartItemId, userId);
                        return new NotFoundException("Cart item not found.");
                    });

            log.debug("5 success");

            // 6
            if (!cartItem.getProduct().isQuantitySufficient(cartItem.getQuantity())) {
                log.warn("Product quantity insufficient for cart item with id = {} on product with id = {}",
                        cartItem.getId(), cartItem.getProduct().getId());
                throw new ConflictException("Product quantity insufficient.");
            }

            log.debug("6 success");

            // 7
            Product product = cartItem.getProduct();
            int newStock = product.getStockQuantity() - cartItem.getQuantity();
            product.setStockQuantity(newStock);

            productRepository.save(product);

            log.debug("7 success");

            // 8
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(null);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
                // Price of order item TODO: create discount
            orderItem.setPriceAtOrder(cartItem.getProduct().getPriceInRupiah());

                // Add to orderItems
            orderItems.add(orderItem);

            log.debug("8 success");
        }

        // 9. Create Order entity (insert all OrderItems)
        Order order = new Order(
                user,
                generateOrderNumber(userId),
                shippingAddress
        );

        log.debug("9 success");

        // 10. Set FK to each OrderItem & add to Order
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
            order.addItem(orderItem);
        }

        orderRepository.save(order);

        log.debug("10 success");

        // 11. Create Payment and save
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment method selected = {}", request.getPaymentMethod());
            throw new BadRequestException("Invalid payment method: " + request.getPaymentMethod());
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalPrice());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(paymentMethod);

        paymentRepository.save(payment);

        log.debug("11 success");

        // 12. Set the payment to Order Entity
        order.setPayment(payment);
        orderRepository.save(order);

        log.debug("12 success");

        // Get order with all relation
        Order finalOrder = orderRepository
                .findUserOrderByIdWithRelation(order.getId(), userId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {}", order.getId());
                    return new NotFoundException("Order not found.");
                });

        log.info("User {} successfully created order {} - TotalItems = {}",
                finalOrder.getId(), userId, finalOrder.getTotalItems());

        return mapToOrderResponse(finalOrder);
    }

    /**
     * Cancel the order and RESTORE product stock
     * Only allowed if order is still pending
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        log.debug("Processing cancel order - User: {}, OrderId: {}",
                userId, orderId);

        Order order = orderRepository.findUserOrderByIdWithRelation(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {} and user id = {}", orderId, userId);
                    return new NotFoundException("Order not found with id = " + orderId);
                });

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            log.warn("Cannot cancel order with id {} because status is {}",
                    orderId, order.getOrderStatus());
            throw new ConflictException("Only PENDING order can be cancelled");
        }

        // Restore stock
        for (OrderItem orderItem : order.getItems()) {
            Product product = orderItem.getProduct();
            int restoredStock = product.getStockQuantity() + orderItem.getQuantity();
            product.setStockQuantity(restoredStock);
            productRepository.save(product);
        }

        OrderStatus oldOrderStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.getPayment().setPaymentStatus(PaymentStatus.FAILED);

        log.info("Order {} successfully set status from {} to {}",
                orderId, oldOrderStatus, order.getOrderStatus());

        orderRepository.save(order);

        log.info("User {} successfully cancelled order {}. Stock restored.",
                orderId, userId);

        return mapToOrderResponse(order);
    }

    /***
     * Get all user orders by user id
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<OrderResponse> getUserOrders(Long userId, OrderFilterRequest filter) {
        log.debug("Processing get user orders - " +
                "User: {}, OrderStatus: {}, FromDate: {}, ToDate: {}",
                userId, filter.getStatus(), filter.getFromDate(), filter.getToDate());

        // Convert filter.status to OrderStatus -- NULLABLE
        OrderStatus orderStatus = null;
        if (filter.getStatus() != null) {
            try {
                orderStatus = OrderStatus.valueOf(filter.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", filter.getStatus());
                throw new BadRequestException("Invalid order status.");
            }
        }

        // Choose sort by CreatedAt
        Sort sort = filter.getSort().equalsIgnoreCase("OLDEST")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                sort
        );

        // Convert LocalDate into LocalDateTime
        LocalDateTime fromDate = filter.getFromDate() != null
                ? filter.getFromDate().atStartOfDay()
                : null;
        LocalDateTime toDate = filter.getToDate() != null
                ? filter.getToDate().atTime(LocalTime.MAX)
                : null;

        Page<Order> orderPage;

        if (fromDate != null && toDate != null) {
            orderPage = orderRepository.findUserOrdersBetweenDateWithRelation(
                    userId,
                    orderStatus,
                    fromDate,
                    toDate,
                    pageable
            );
        } else if (fromDate != null) {
            orderPage = orderRepository.findUserOrdersFromDateWithRelation(
                    userId,
                    orderStatus,
                    fromDate,
                    pageable
            );
        } else if (toDate != null) {
            orderPage = orderRepository.findUserOrdersToDateWithRelation(
                    userId,
                    orderStatus,
                    toDate,
                    pageable
            );
        } else {
            orderPage = orderRepository.findUserOrdersWithRelation(
                    userId,
                    orderStatus,
                    pageable
            );
        }

        log.info("User {} successfully got {} orders ",
                userId, orderPage.getTotalElements());

        return mapPageToResponse(orderPage);
    }

    /**
     * CUSTOMER - Get single user order by id and user id
     */
    @Transactional(readOnly = true)
    public OrderResponse getUserOrderById(Long userId, Long orderId) {

        log.debug("Processing get user order by id - User: {}, OrderId: {}",
                userId, orderId);

        Order order = orderRepository.findUserOrderByIdWithRelation(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {} and user id = {}", orderId, userId);
                    return new NotFoundException("Order not found.");
                });

        log.info("User {} successfully got order {}", userId, orderId);

        return mapToOrderResponse(order);
    }

    // -------------------------
    // --- ADMIN METHODS ---
    // -------------------------

    /**
     * ADMIN - Search filtered orders from every user
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<OrderResponse> adminSearchAllOrders(OrderFilterRequest filter) {

        log.debug("Processing admin search all orders - Status: {}, FromDate: {}, ToDate: {}",
                filter.getStatus(), filter.getFromDate(), filter.getToDate());

        // Convert filter.status to OrderStatus -- NULLABLE
        OrderStatus orderStatus = null;
        if (filter.getStatus() != null) {
            try {
                orderStatus = OrderStatus.valueOf(filter.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", filter.getStatus());
                throw new BadRequestException("Invalid order status.");
            }
        }

        // Choose sort by CreatedAt
        Sort sort = filter.getSort().equalsIgnoreCase("OLDEST")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                sort
        );

        // Convert LocalDate into LocalDateTime
        LocalDateTime fromDate = filter.getFromDate() != null
                ? filter.getFromDate().atStartOfDay()
                : null;
        LocalDateTime toDate = filter.getToDate() != null
                ? filter.getToDate().atTime(LocalTime.MAX)
                : null;

        Page<Order> orderPage;

        if (fromDate != null && toDate != null) {
            orderPage = orderRepository.findAllOrdersBetweenDateWithRelation(
                    orderStatus,
                    fromDate,
                    toDate,
                    pageable
            );
            log.debug("Running query -> orderRepository.findAllOrdersBetweenDateWithRelation");

        } else if (fromDate != null) {
            orderPage = orderRepository.findAllOrdersFromDateWithRelation(
                    orderStatus,
                    fromDate,
                    pageable
            );
            log.debug("Running query -> orderRepository.findAllOrdersFromDateWithRelation");

        } else if (toDate != null) {
            orderPage = orderRepository.findAllOrdersToDateWithRelation(
                    orderStatus,
                    toDate,
                    pageable
            );
            log.debug("Running query -> orderRepository.findAllOrdersToDateWithRelation");


        } else {
            orderPage = orderRepository.findAllOrdersWithRelation(
                    orderStatus,
                    pageable
            );
            log.debug("Running query -> orderRepository.findAllOrdersWithRelation");

        }

        log.info("Admin successfully got all {} orders",
                orderPage.getTotalElements());

        return mapPageToResponse(orderPage);
    }

    /**
     * ADMIN - get order
     * 
     */
    @Transactional(readOnly = true)
    public OrderResponse adminGetOrderById(Long orderId) {

        log.debug("Processing admin get order by id - Order: {}", orderId);

        Order order = orderRepository
                .findOrderByIdWithRelationForAdmin(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {}", orderId);
                    return new NotFoundException("Order not found.");
                });

        log.info("Admin successfully got order with id {}", orderId);

        return mapToOrderResponse(order);
    }

    /**
     * Update order status to ship
     */
    @Transactional
    public OrderResponse adminUpdateOrderStatusToShip(Long orderId, UpdateOrderStatusToShipRequest request) {

        log.debug("Processing admin update order status to ship - " +
                "Order: {}, ShippingProvider: {}, TrackingNumber: {}",
                orderId, request.getShippingProvider(), request.getTrackingNumber());

        OrderStatus targetStatus = OrderStatus.SHIPPED;

        Order order = orderRepository
                .findOrderByIdWithRelationForAdmin(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {}", orderId);
                    return new NotFoundException("Order not found.");
                });

        // Check if the current order status can be transitioned
        OrderStatus currentStatus = order.getOrderStatus();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            log.warn("Invalid status transition from {} to {} for order id = {}",
                    targetStatus, currentStatus, orderId);
            throw new ConflictException("Cannot change order status from " +
                    currentStatus + " to " + targetStatus);
        }

        // Set order status & add some shipping order information
        order.setOrderStatus(targetStatus);
        order.setShippingProvider(request.getShippingProvider());
        order.setTrackingNumber(request.getTrackingNumber());

        orderRepository.save(order);

        log.info("Admin successfully updated order status from {} to {} - Order: {}",
                currentStatus, targetStatus, orderId);

        return mapToOrderResponse(order);
    }

    /**
     * Update order status to complete
     */
    @Transactional
    public OrderResponse adminUpdateOrderStatusToComplete(Long orderId) {

        log.debug("Processing admin update order status to complete - Order: {}", orderId);

        OrderStatus targetStatus = OrderStatus.COMPLETED;

        Order order = orderRepository
                .findOrderByIdWithRelationForAdmin(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {}", orderId);
                    return new NotFoundException("Order not found.");
                });

        // Check if the current order status can be transitioned
        OrderStatus currentStatus = order.getOrderStatus();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            log.warn("Invalid status transition from {} to {} for order id = {}",
                    targetStatus, currentStatus, orderId);
            throw new ConflictException("Cannot change order status from " +
                    currentStatus + " to " + targetStatus);
        }

        order.setOrderStatus(targetStatus);
        orderRepository.save(order);

        log.info("Admin successfully updated order status from {} to {} - Order: {}",
                currentStatus, targetStatus, orderId);

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse adminUpdateOrderStatusToCancel(Long orderId) {

        log.debug("Processing admin update order status to cancel - Order: {}", orderId);

        OrderStatus targetStatus = OrderStatus.CANCELLED;

        Order order = orderRepository
                .findOrderByIdWithRelationForAdmin(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {}", orderId);
                    return new NotFoundException("Order not found.");
                });

        // Check if the current order status can be transitioned
        OrderStatus currentStatus = order.getOrderStatus();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            log.warn("Invalid status transition from {} to {} for order id = {}",
                    targetStatus, currentStatus, orderId);
            throw new ConflictException("Cannot change order status from " +
                    currentStatus + " to " + targetStatus);
        }

        order.setOrderStatus(targetStatus);
        orderRepository.save(order);

        log.info("Admin successfully updated order status from {} to {} - Order: {}",
                currentStatus, targetStatus, orderId);

        return mapToOrderResponse(order);
    }


    private String generateOrderNumber(Long userId) {
        return "ORD-" + System.currentTimeMillis() + "-" + userId;
    }

    private PaginatedResponse<OrderResponse> mapPageToResponse(Page<Order> orderPage) {

        PaginatedResponse<OrderResponse> response = new PaginatedResponse<>();
        // Map Page<Product> into List<ProductResponse>
        response.setContent(orderPage.map(this::mapToOrderResponse).toList());
        response.setPageNumber(orderPage.getNumber());
        response.setPageSize(orderPage.getSize());
        response.setTotalPages(orderPage.getTotalPages());
        response.setTotalElements(orderPage.getTotalElements());
        response.setHasNextPage(orderPage.hasNext());
        response.setHasPreviousPage(orderPage.hasPrevious());

        return response;
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        log.debug("set id");
        response.setOrderNumber(order.getOrderNumber());
        log.debug("set order number");
        response.setOrderStatus(order.getOrderStatus().toString());
        log.debug("set order status");
        response.setPaymentStatus(order.getPayment().getPaymentStatus().toString());
        log.debug("set payment status");
        response.setTotalItems(order.getTotalItems());
        log.debug("set total items");
        response.setTotalPrice(order.getTotalPrice());
        log.debug("set total price");
        response.setCreatedAt(order.getCreatedAt());
        log.debug("set created at");

        response.setItems(order.getItems().stream()
                .map(oi -> new OrderResponse.OrderItemResponse(
                        oi.getId(),
                        oi.getProduct().getId(),
                        oi.getProduct().getName(),
                        oi.getProduct().getImageUrl(),
                        oi.getQuantity(),
                        oi.getPriceAtOrder(),
                        oi.getSubtotal()
                )).toList()
        );
        log.debug("set items");

        response.setShippingAddress(new AddressResponse(
                order.getShippingAddress().getId(),
                order.getShippingAddress().getRecipientName(),
                order.getShippingAddress().getPhoneNumber(),
                order.getShippingAddress().getStreet(),
                order.getShippingAddress().getCity(),
                order.getShippingAddress().getProvince(),
                order.getShippingAddress().getPostalCode(),
                order.getShippingAddress().getNotes()
        ));
        log.debug("set shipping address");

        return response;
    }
}
