package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.domain.OrderStatus;
import com.techgadget.ecommerce.dto.request.CreateOrderRequest;
import com.techgadget.ecommerce.dto.request.OrderFilterRequest;
import com.techgadget.ecommerce.dto.response.ErrorResponse;
import com.techgadget.ecommerce.dto.response.OrderResponse;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.security.CustomUserDetails;
import com.techgadget.ecommerce.service.OrderService;
import com.techgadget.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * Create order from selected cart items.
     * POST /api/orders
     * -
     * Example request body:
     * {
     *   "addressId": 1,
     *   "cartItemIds": [1, 2, 3]
     * }
     */
    @Operation(
            summary = "Create new order",
            description = "Create order from selected cart items"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order is created"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request
    ) {

        OrderResponse response = orderService
                .createOrder(userDetails.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancel order (manual cancel) and RESTORE product stock
     * Only allowed if order status is PENDING
     * POST /api/orders/{orderId}/cancel
     */
    @Operation(
            summary = "Cancel order",
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
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {

        OrderResponse response = orderService
                .cancelOrder(userDetails.getUserId(), orderId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Logic of Order payment (DUMMY)
     * Mark order as PAID and CONFIRMED
     * POST /api/orders/{orderId}/pay
     */
    @Operation(
            summary = "Pay order",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order is paid and payment is created"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<OrderResponse> payOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {
        // TODO: Create PaymentService

        // After payment successes, MARK order
        OrderResponse response = paymentService
                .payOrder(userDetails.getUserId(), orderId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all user orders
     * GET /api/orders
     */
    @Operation(
            summary = "Get user orders",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = ""),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute OrderFilterRequest filter
    ) {


        PaginatedResponse<OrderResponse> response = orderService
                .getUserOrders(userDetails.getUserId(), filter);

        return ResponseEntity.ok(response);
    }

    /**
     * Get single user order
     * GET /api/orders/{orderId}
     * -
     */
    @Operation(
            summary = "Get order by id",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order by id is founded"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping("{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {

        OrderResponse response = orderService
                .getUserOrderById(userDetails.getUserId(), orderId);

        return ResponseEntity.ok(response);
    }


}
