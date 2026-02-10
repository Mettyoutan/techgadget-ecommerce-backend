package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.domain.OrderStatus;
import com.techgadget.ecommerce.domain.PaymentStatus;
import com.techgadget.ecommerce.dto.response.AddressResponse;
import com.techgadget.ecommerce.dto.response.OrderResponse;
import com.techgadget.ecommerce.entity.Order;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.OrderRepository;
import com.techgadget.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Using DUMMY payment
     * After payment success, mark order as PAID and CONFIRMEDa
     * Order is CONFIRMED
     */
    @Transactional
    public OrderResponse payOrder(Long userId, Long orderId) {

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

        // USING DUMMY PAYMENT
        // Set payment status & order status
        order.getPayment().setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.CONFIRMED);

        orderRepository.save(order);

        log.info("Order id = {} marked as PAID and COMPLETED by user id = {}", orderId, userId);

        return mapToOrderResponse(order);
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
