package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.OrderFilterRequest;
import com.techgadget.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.techgadget.ecommerce.dto.response.OrderResponse;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Validated
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Get all orders from every user
     * -
     * Provide query params for filter
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<OrderResponse>> searchOrders(
            @Valid @ModelAttribute OrderFilterRequest filter
    ) {

        PaginatedResponse<OrderResponse> response =
                orderService.searchAllOrders(filter);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all user orders
     * -
     * With user id
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<PaginatedResponse<OrderResponse>> searchUserOrders(
            @Valid @ModelAttribute OrderFilterRequest filter,
            @PathVariable Long userId
    ) {
        PaginatedResponse<OrderResponse> response =
                orderService.getUserOrders(userId, filter);

        return ResponseEntity.ok(response);
    }

    /**
     * Get order by id
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId
    ) {

        OrderResponse response = orderService.getOrderByIdForAdmin(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update order status
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {

        OrderResponse response = orderService
                .updateOrderStatus(orderId, request.getStatus());

        return ResponseEntity.ok(response);
    }
}
