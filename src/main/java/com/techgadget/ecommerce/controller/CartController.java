package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.AddCartItemRequest;
import com.techgadget.ecommerce.dto.request.UpdateCartItemRequest;
import com.techgadget.ecommerce.dto.response.CartResponse;
import com.techgadget.ecommerce.dto.response.ErrorResponse;
import com.techgadget.ecommerce.security.CustomUserDetails;
import com.techgadget.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CartController {

    private final CartService cartService;

    /**
     * Get current user's cart
     * GET /api/cart (CUSTOMER)
     */
    @Operation(
            summary = "Get user's cart",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User's cart is founded"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse response = cartService.getCart(userDetails.getUserId());

        return ResponseEntity.ok(response);
    }

    /**
     * Add item to cart
     * POST /api/cart (CUSTOMER)
     *
     * Request body = AddCartItemRequest dto
     */
    @Operation(
            summary = "Add cart item to cart",
            description = "Create cart item and add it to the user's cart"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cart item created and added"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PostMapping
    public ResponseEntity<CartResponse> addCartItemToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        CartResponse response = cartService.addToCart(userDetails.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update cart item in cart
     * PUT /api/cart/{cartItemId} (CUSTOMER)
     *
     * Request body = UpdateCartItemRequest
     */
    @Operation(
            summary = "Update cart item",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item is updated"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PutMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCartItemRequest request,
            @PathVariable Long cartItemId
    ) {
        CartResponse response = cartService.updateCartItem(
                userDetails.getUserId(), cartItemId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove cart item in cart
     * DELETE /api/cart/{cartItemId} (CUSTOMER)
     */
    @Operation(
            summary = "Delete cart item in cart",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item is deleted"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId
    ) {
        CartResponse response = cartService.removeCartItem(
                userDetails.getUserId(), cartItemId);

        return ResponseEntity.ok(response);
    }

    /**
     * Clear all cart items
     * DELETE /api/cart (CUSTOMER)
     */
    @Operation(
            summary = "Clear all cart items",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All cart items is cleared"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Clear cart endpoint hit.");
        CartResponse response =
                cartService.clearCart(userDetails.getUserId());

        return ResponseEntity.ok(response);
    }

    /**
     * Count total cart items
     * GET /api/cart/count (CUSTOMER)
     */
    @Operation(
            summary = "Count cart items",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart items are counted"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping("/count")
    public ResponseEntity<CartResponse.CartCountResponse> getCartItemCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse.CartCountResponse response = cartService.getCartItemCount(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
