package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.order.OrderFilterRequest;
import com.techgadget.ecommerce.dto.request.order.UpdateOrderStatusToShipRequest;
import com.techgadget.ecommerce.dto.response.ErrorResponse;
import com.techgadget.ecommerce.dto.response.order.OrderResponse;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Admin search all orders",
            description = "Admin search all orders using query params filter"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders is found"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<OrderResponse>> searchAllOrders(
            @Valid @ModelAttribute OrderFilterRequest filter
    ) {

        PaginatedResponse<OrderResponse> response =
                orderService.adminSearchAllOrders(filter);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all user orders
     * -
     * With user id
     */
    @Operation(
            summary = "Admin search user orders",
            description = "Cancel order (manual cancel) and RESTORE product stock"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order is canceled"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
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
    @Operation(
            summary = "Admin get order by id",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order is found"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId
    ) {

        OrderResponse response = orderService.adminGetOrderById(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update order status to confirmed
     */
    @Operation(
            summary = "Admin update order status to confirmed",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status is updated"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> updateOrderStatusToConfirmed(
            @PathVariable Long orderId
    ) {

        OrderResponse response = orderService
                .adminUpdateOrderStatusToConfirmed(orderId);

        return ResponseEntity.ok(response);
    }

    /**
     * Update order status to ship
     */
    @Operation(
            summary = "Admin update order status to shipped",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status is updated"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PatchMapping("/{orderId}/ship")
    public ResponseEntity<OrderResponse> updateOrderStatusToShipped(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusToShipRequest request
    ) {

        OrderResponse response = orderService
                .adminUpdateOrderStatusToShipped(orderId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Update order status to complete
     */
    @Operation(
            summary = "Admin update order status to completed",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status is updated"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<OrderResponse> updateOrderStatusToCompleted(
            @PathVariable Long orderId
    ) {

        OrderResponse response = orderService
                .adminUpdateOrderStatusToCompleted(orderId);

        return ResponseEntity.ok(response);
    }

    /**
     * Admin cancel customer order
     * -
     * Update order status to cancel
     */
    @Operation(
            summary = "Update order status to cancelled",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status is updated"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> updateOrderStatusToCancelled(
            @PathVariable Long orderId
    ) {

        OrderResponse response = orderService
                .adminUpdateOrderStatusToCancelled(orderId);

        return ResponseEntity.ok(response);
    }


}
