package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.AddCartItemRequest;
import com.techgadget.ecommerce.dto.request.UpdateCartItemRequest;
import com.techgadget.ecommerce.dto.response.CartResponse;
import com.techgadget.ecommerce.security.CustomUserDetails;
import com.techgadget.ecommerce.service.CartService;
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
    @GetMapping("/count")
    public ResponseEntity<CartResponse.CartCountResponse> getCartItemCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse.CartCountResponse response = cartService.getCartItemCount(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
