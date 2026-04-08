package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.enums.OrderStatus;
import com.techgadget.ecommerce.enums.PaymentMethod;
import com.techgadget.ecommerce.enums.PaymentStatus;
import com.techgadget.ecommerce.dto.request.order.CreateOrderRequest;
import com.techgadget.ecommerce.dto.request.order.OrderFilterRequest;
import com.techgadget.ecommerce.dto.request.order.UpdateOrderStatusToShipRequest;
import com.techgadget.ecommerce.dto.response.user.AddressResponse;
import com.techgadget.ecommerce.dto.response.order.OrderResponse;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.entity.*;
import com.techgadget.ecommerce.enums.UserRole;
import com.techgadget.ecommerce.exception.BadRequestException;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.*;
import jakarta.annotation.Nullable;
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
    private final ProductImageService productImageService;

    // -------------------------
    // --- CUSTOMER METHODS ---
    // -------------------------

    /**
     * CUSTOMER
     * - Create PENDING order using selected cart items, not all.
     * - Product stock is decreased when order is created (status PENDING).
     * Cart items are not getting removed.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {

        log.debug("Processing create order - " +
                        "User: {}, CartItem: {}, Address: {}, PaymentMethod: {}",
                userId,
                request.getCartItemIds(),
                request.getAddressId(),
                request.getPaymentMethod());

        // Make sure item not empty (Preventive - even already checked by Java Validation)
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
            if (!cartItem.getProduct().isStockSufficient(cartItem.getQuantity())) {
                log.warn("Product quantity insufficient for cart item with id = {} on product with id = {}",
                        cartItem.getId(), cartItem.getProduct().getId());
                throw new ConflictException("Product quantity insufficient.");
            }

            log.debug("6 success");

            // 7
            Product product = cartItem.getProduct();
            int newStock = product.getStock() - cartItem.getQuantity();
            product.setStock(newStock);

            productRepository.save(product);

            log.debug("7 success");

            /*
                8 - Create OrderItem
             */
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(null);
            orderItem.setProductIdSnapshot(product.getId());
            orderItem.setProductNameSnapshot(product.getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtOrder(product.getPrice());

            // Find single image for OrderItem
            ProductImage selectedImage = product.getImages()
                    .stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .orElse(null);

            String imageKey = null;
            if (selectedImage != null) {
                imageKey = selectedImage.getThumbnailKey() != null
                        ? selectedImage.getThumbnailKey()
                        : selectedImage.getOriginalKey();
            }
            orderItem.setProductImageKeySnapshot(imageKey);

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

        order = orderRepository.save(order);

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
        payment.setPaymentStatus(PaymentStatus.PENDING); // PENDING order
        payment.setPaymentMethod(paymentMethod);

        payment = paymentRepository.save(payment);

        log.debug("11 success");

        // 12. Set the payment to Order Entity
        order.setPayment(payment);
        order = orderRepository.save(order);

        log.debug("12 success");

        // Get order with all relation
        Order tempOrder = order;
        Order finalOrder = orderRepository
                .findUserOrderById(order.getId(), userId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found", tempOrder.getId());
                    return new NotFoundException("Order not found.");
                });

        log.info("User {} successfully created order {} - TotalItems={}",
                finalOrder.getId(), userId, finalOrder.getTotalItems());
        log.debug("OrderNumber:{}, OrderStatus:{}, Payment: {}",
                finalOrder.getOrderNumber(), finalOrder.getOrderStatus(), finalOrder.getPayment());

        return mapToOrderResponse(finalOrder);
    }

    /**
     * CUSTOMER
     * - Cancel the (PENDING, PROCESSING) unshipped order.
     * - Product stock is restored.
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        log.debug("Processing cancel order: User={}, Order={}",
                userId, orderId);

        Order order = orderRepository.findUserOrderById(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {} and user id = {}", orderId, userId);
                    return new NotFoundException("Order not found.");
                });

        // Customer can only cancel UNSHIPPED order (PENDING, PROCESSING)
        OrderStatus orderStatus = order.getOrderStatus();
        if (!orderStatus.canTransitionTo(OrderStatus.CANCELLED, UserRole.CUSTOMER)) {
            log.warn("Can't cancel order {} because current order status is {}, not UNSHIPPED order.",
                    orderId, order.getOrderStatus());
            throw new ConflictException("Order cannot be cancelled.");
        }

        // Restore stock
        for (OrderItem orderItem : order.getItems()) {
            // Get the product
            Product product = productRepository
                    .findById(orderItem.getProductIdSnapshot())
                    .orElseThrow(() -> {
                        log.warn("Product with id snapshot {} not found: Order={}, OrderItem:{}",
                                orderItem.getProductIdSnapshot(), orderId, orderItem.getId());
                        return new NotFoundException("Product id snapshot not found.");
                    });

            // Set restored Stock quantity
            int restoredStock = product.getStock() + orderItem.getQuantity();
            product.setStock(restoredStock);
            productRepository.save(product);

        }

        // Set new order status
        OrderStatus oldOrderStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.CANCELLED);

        // Set new payment status
        // TODO: If order is PAID, implement REFUND logic
        PaymentStatus paymentStatus = order.getPayment().getPaymentStatus();
        if (paymentStatus == PaymentStatus.PAID) {
            // TODO: SOME REFUND LOGIC
            // Trigger refund to payment gateway
            order.getPayment().setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            order.getPayment().setPaymentStatus(PaymentStatus.FAILED);
        }

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
        @Nullable OrderStatus orderStatus = null;
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
            orderPage = orderRepository.findUserOrdersBetweenDate(
                    userId,
                    orderStatus,
                    fromDate,
                    toDate,
                    pageable
            );
        } else if (fromDate != null) {
            orderPage = orderRepository.findUserOrdersFromDate(
                    userId,
                    orderStatus,
                    fromDate,
                    pageable
            );
        } else if (toDate != null) {
            orderPage = orderRepository.findUserOrdersToDate(
                    userId,
                    orderStatus,
                    toDate,
                    pageable
            );
        } else {
            orderPage = orderRepository.findUserOrders(
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

        Order order = orderRepository.findUserOrderById(orderId, userId)
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
     * Admin update order status into PROCESSING.
     * Available for PAID order
     */
    @Transactional
    public OrderResponse adminUpdateOrderStatusToProcessing(Long orderId) {

        log.debug("Admin processing update order status to PROCESSING - Order: {}", orderId);

        OrderStatus target = OrderStatus.PROCESSING;

        Order order = orderRepository
                .findOrderByIdWithRelationForAdmin(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found", orderId);
                    return new NotFoundException("Order not found.");
                });

        // Payment must be PAID
        if (!order.getPayment().getPaymentStatus().equals(PaymentStatus.PAID)) {
            log.warn("Order {} has not been paid, so can't be confirmed", orderId);
            throw new ConflictException("Cannot process order that has not been paid.");
        }

        OrderStatus current = order.getOrderStatus();

        if (!current.canTransitionTo(target, UserRole.ADMIN)) {
            log.warn("Invalid status transition from {} to {} for order id = {}",
                    target, current, orderId);
            throw new ConflictException("Order can't be processed.");
        }

        order.setOrderStatus(target);
        orderRepository.save(order);

        log.info("Admin successfully updated order status from {} to {}: Order={}",
                current, target, orderId);

        return mapToOrderResponse(order);
    }

    /**
     * Update order status into SHIPPED
     */
    @Transactional
    public OrderResponse adminUpdateOrderStatusToShipped(Long orderId, UpdateOrderStatusToShipRequest request) {

        log.debug("Admin processing update order status to SHIPPED: " +
                "Order: {}, ShippingProvider: {}, TrackingNumber: {}",
                orderId, request.getShippingProvider(), request.getTrackingNumber());

        OrderStatus target = OrderStatus.SHIPPED;

        Order order = orderRepository
                .findOrderByIdWithRelationForAdmin(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {}", orderId);
                    return new NotFoundException("Order not found.");
                });

        // Check if the current order status can be transitioned
        OrderStatus current = order.getOrderStatus();

        if (!current.canTransitionTo(target, UserRole.ADMIN)) {
            log.warn("Invalid status transition from {} to {} for order id = {}",
                    target, current, orderId);
            throw new ConflictException("Order can't shipped.");
        }

        // Set order status & add some shipping order information
        order.setOrderStatus(target);
        order.setShippingProvider(request.getShippingProvider());
        order.setTrackingNumber(request.getTrackingNumber());

        orderRepository.save(order);

        log.info("Admin successfully updated order status from {} to {} - Order: {}",
                current, target, orderId);

        return mapToOrderResponse(order);
    }

    /**
     * Update order status into COMPLETED
     */
    @Transactional
    public OrderResponse adminUpdateOrderStatusToCompleted(Long orderId) {

        log.debug("Admin processing update order status to complete - Order: {}", orderId);

        OrderStatus target = OrderStatus.COMPLETED;

        Order order = orderRepository
                .findOrderByIdWithRelationForAdmin(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {}", orderId);
                    return new NotFoundException("Order not found.");
                });

        // Check if the current order status can be transitioned
        OrderStatus current = order.getOrderStatus();

        if (!current.canTransitionTo(target, UserRole.ADMIN)) {
            log.warn("Invalid status transition from {} to {} for order id = {}",
                    target, current, orderId);
            throw new ConflictException("Order can't be completed.");
        }

        order.setOrderStatus(target);
        orderRepository.save(order);

        log.info("Admin successfully updated order status from {} to {} - Order: {}",
                current, target, orderId);

        return mapToOrderResponse(order);
    }

    /**
     * Update order status into CANCELLED
     */
    @Transactional
    public OrderResponse adminUpdateOrderStatusToCancelled(Long orderId) {

        log.debug("Processing admin update order status to cancel - Order: {}", orderId);

        OrderStatus target = OrderStatus.CANCELLED;

        Order order = orderRepository
                .findOrderByIdWithRelationForAdmin(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {}", orderId);
                    return new NotFoundException("Order not found.");
                });

        // Check if the current order status can be transitioned
        OrderStatus current = order.getOrderStatus();

        if (!current.canTransitionTo(target, UserRole.ADMIN)) {
            log.warn("Invalid status transition from {} to {} for order id = {}",
                    target, current, orderId);
            throw new ConflictException("Order can't be cancelled.");
        }

        order.setOrderStatus(target);
        orderRepository.save(order);

        log.info("Admin successfully updated order status from {} to {} - Order: {}",
                current, target, orderId);

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

        List<OrderResponse.OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {

            // Get image url
            String imageUrl = null;
            String imageKey = item.getProductImageKeySnapshot();
            if (imageKey != null) {
                imageUrl = productImageService.getImageUrl(imageKey);
            }

            // Build ItemResponse
            var itemRes = new OrderResponse.OrderItemResponse(
                    item.getId(),
                    item.getProductIdSnapshot(),
                    item.getProductNameSnapshot(),
                    imageUrl,
                    item.getQuantity(),
                    item.getPriceAtOrder(),
                    item.getSubtotal()
            );

            itemResponses.add(itemRes);
        }

        // Build AddressResponse
        Address address = order.getShippingAddress();
        AddressResponse addressRes = new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getStreet(),
                address.getCity(),
                address.getProvince(),
                address.getPostalCode(),
                address.getNotes()
        );

        // Build OrderResponse
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus().toString())
                .paymentStatus(order.getPayment().getPaymentStatus().toString())
                .totalPrice(order.getTotalPrice())
                .totalItems(order.getTotalItems())
                .createdAt(order.getCreatedAt())
                .shippingAddress(addressRes)
                .items(itemResponses)
                .build();
    }
}
