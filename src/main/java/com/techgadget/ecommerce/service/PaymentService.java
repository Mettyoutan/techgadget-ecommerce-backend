package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.entity.OrderItem;
import com.techgadget.ecommerce.enums.OrderStatus;
import com.techgadget.ecommerce.enums.PaymentStatus;
import com.techgadget.ecommerce.dto.response.user.AddressResponse;
import com.techgadget.ecommerce.dto.response.order.OrderResponse;
import com.techgadget.ecommerce.entity.Order;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.OrderRepository;
import com.techgadget.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductImageService productImageService;

    /**
     * Using DUMMY payment
     * After payment success, mark order as PAID and CONFIRMEDa
     * Order is CONFIRMED
     */
    @Transactional
    public OrderResponse payOrder(Long userId, Long orderId) {

        log.debug("Processing pay order - User: {}, Order: {}",
                userId, orderId);

        Order order = orderRepository.findUserOrderById(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id = {} and user id = {}", orderId, userId);
                    return new NotFoundException("Order not found with id = " + orderId);
                });

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            log.warn("Cannot cancel order with id {} because status is {}",
                    orderId, order.getOrderStatus());
            throw new ConflictException("Only PENDING order can be cancelled");
        }

        // USING DUMMY PAYMENT
        // Set payment status & order status
        order.getPayment().setPaymentStatus(PaymentStatus.PAID); // PAID
        order.setOrderStatus(OrderStatus.PAID); // PAID

        orderRepository.save(order);

        log.info("Order {} marked as PAID by User {}", orderId, userId);

        return mapToOrderResponse(order);
    }

    private OrderResponse mapToOrderResponse(Order order) {

        // Build list of OrderItemResponse
        List<OrderResponse.OrderItemResponse> itemResList = new ArrayList<>();
        for (OrderItem item : order.getItems()) {

            // Get order image URL
            String imageUrl = null;
            String imageKey = item.getProductImageKeySnapshot();
            if (imageKey != null) {
                imageUrl = productImageService.getImageUrl(imageKey);
            }

            itemResList.add(new OrderResponse.OrderItemResponse(
                        item.getId(),
                        item.getProductIdSnapshot(),
                        item.getProductNameSnapshot(),
                        imageUrl, // Image URL
                        item.getQuantity(),
                        item.getPriceAtOrder(),
                        item.getSubtotal()
            ));
        }

        // Build shipping AddressResponse
        AddressResponse shippingAddressRes = new AddressResponse(
                order.getShippingAddress().getId(),
                order.getShippingAddress().getRecipientName(),
                order.getShippingAddress().getPhoneNumber(),
                order.getShippingAddress().getStreet(),
                order.getShippingAddress().getCity(),
                order.getShippingAddress().getProvince(),
                order.getShippingAddress().getPostalCode(),
                order.getShippingAddress().getNotes()
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
                .shippingAddress(shippingAddressRes)
                .items(itemResList)
                .build();
    }
}
