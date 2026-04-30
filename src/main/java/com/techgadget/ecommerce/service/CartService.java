package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.request.cart.AddCartItemRequest;
import com.techgadget.ecommerce.dto.request.cart.UpdateCartItemRequest;
import com.techgadget.ecommerce.dto.response.cart.CartResponse;
import com.techgadget.ecommerce.entity.*;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageService productImageService;

    /**
     * Helper method to get cart entity
     * If not exists (null), create new cart
     */
    private Cart getOrCreateCartEntity(Long userId) {

        // Get cart with OPTIMIZED QUERY --> JOIN FETCH items
        Cart cart = cartRepository.findByUser_IdWithItems(userId).orElse(null);

        if (cart == null) {
            log.debug("Cart not found, creating.");
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found with id = {}.", userId);
                        return new NotFoundException("User not found.");
                    });

            // Create new cart for user
            cart = cartRepository.save(new Cart(user));
        }

        log.debug("Cart retrieved/created. [cartId={}]", cart.getId());

        return cart;
    }

    /**
     * Get cart with user ID
     */
    @Transactional
    public CartResponse getCart(Long userId) {

        Cart cart = getOrCreateCartEntity(userId);
        return mapToCartResponse(cart);
    }

    /**
     * Add cart item to cart
     */
    @Transactional
    public CartResponse addToCart(Long userId, AddCartItemRequest request) {
        log.debug("Adding items to cart. [productId={}, itemQuantity={}]",
                request.getProductId(), request.getQuantity());

        // Get or create cart
        Cart cart = getOrCreateCartEntity(userId);

        // Get product & check stock
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> {
                    log.warn("Product not found. [productId={}]", request.getProductId());
                    return new NotFoundException("Product not found.");
                });

        /**
         * Check if cart item already exists
         * If yes, increase the quantity
         * If not, create new
         */
        CartItem cartItem = cartItemRepository.findByCart_IdAndProduct_IdWithProduct(
                cart.getId(), product.getId()).orElse(null);

        if (cartItem != null) {
            // Update new quantity to existing cart item
            int oldQuantity = cartItem.getQuantity();
            int newQuantity = oldQuantity + request.getQuantity();

            if (!product.isStockSufficient(newQuantity)) {
                log.warn("Insufficient stock. [productId={}, requested={}, stock={}]",
                        product.getId(), newQuantity, product.getStock());
                throw new ConflictException("Stock quantity not sufficient.");
            }

            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);

//                int index = cart.getItems().indexOf(cartItem);
//                if (index >= 0) {
//                    cart.getItems().set(index, cartItem);  // Force update reference
//                }

            log.debug("Cart item quantity updated. [cartItemId={}, productId={}, oldQuantity={}, newQuantity={}]",
                    cartItem.getId(), product.getId(), oldQuantity, newQuantity);

        } else {
            // Check if product stock not sufficient to quantity
            if (!product.isStockSufficient(request.getQuantity())) {
                log.warn("Insufficient stock. [productId={}, requested={}, stock={}]",
                        product.getId(), request.getQuantity(), product.getStock());
                throw new ConflictException("Stock quantity not sufficient.");
            }

            // Create new cart item & add to cart
            cartItem = new CartItem(cart, product, request.getQuantity());
            cart.addItem(cartItem);

            log.debug("New cart item created. [cartId={}, cartItemId={}, productId={}, quantity={}, totalItems={}]",
                    cart.getId(), cartItem.getId(), product.getId(), request.getQuantity(), cart.getItems().size());
        }

        // Save cart
        cart = cartRepository.save(cart);

        log.info("Item added to cart successfully. " +
                        "[productId={}, cartId={}, quantity={}, totalItems={}]",
                request.getProductId(),
                cart.getId(),
                request.getQuantity(),
                cart.getItems().size()
        );

        return mapToCartResponse(cart);
    }

    /**
     * Update cart item (only quantity)
     */
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        log.debug("Updating cart item. [cartItemId={}, newQuantity={}",
                cartItemId, request.getQuantity());

        // Get cart item using id and user id
        CartItem cartItem = cartItemRepository.findByIdAndCart_User_IdWithProduct(cartItemId, userId)
                .orElseThrow(() -> {
                    log.warn("Cart item not found with id = {} - user id = {}.", cartItemId, userId);
                    return new NotFoundException("Cart item not found.");
                });

        // Set new quantity
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        Cart cart = getOrCreateCartEntity(userId);

        log.info("Cart item updated successfully. [cartId={}, cartItemId={}, quantity={}]",
                cart.getId(),
                cartItemId,
                request.getQuantity()
        );

        return mapToCartResponse(cart);
    }

    /**
     * Remove cart item
     */
    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        log.debug("Removing cart item. [cartItemId={}]", cartItemId);

        Cart cart = getOrCreateCartEntity(userId);

        // Find cart item with id
        CartItem cartItem = cart.getItems()
                .stream()
                .filter(ci -> ci.getId().equals(cartItemId)).findFirst()
                .orElseThrow(() -> {
                    log.warn("Cart item not found. [cartItemId={}]", cartItemId);
                    return new NotFoundException("Cart item not found.");
                });

        // remove and save
        cart.getItems().remove(cartItem);
        cartRepository.save(cart);

        log.info("Cart item removed successfully. [cartId={}, cartItemId={}]", cart.getId(), cartItemId);

        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        log.debug("Clearing cart.");

        Cart cart = getOrCreateCartEntity(userId);
        int itemsCleared = cart.getItems().size();

        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Cart cleared successfully. [cartId={}, itemsCleared={}]",
                cart.getId(), itemsCleared);

        return mapToCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse.CartCountResponse getCartItemCount(Long userId) {
        log.debug("Getting cart items count.");

        Cart cart = getOrCreateCartEntity(userId);
        return new CartResponse.CartCountResponse(cart.getTotalItemsQuantity());
    }

    private CartResponse mapToCartResponse(Cart cart) {

        // Build list of CartItemResponse
        List<CartResponse.CartItemResponse> items = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            // Map to CartItemRes
            items.add(mapToCartItemResponse(cartItem));
        }

        // Create cart response
        return new CartResponse(
                items,
                cart.getTotalPrice(),
                cart.getTotalItemsQuantity()
        );
    }

    private CartResponse.CartItemResponse mapToCartItemResponse(CartItem cartItem) {

        // Get primary image URL
        String primaryImageUrl = null;
        String primaryImageKey = cartItem.getProduct().getPrimaryImageKey();
        if (primaryImageKey != null) {
            primaryImageUrl = productImageService.getImageUrl(primaryImageKey);
        }

        // Build CartItemResponse
        return new CartResponse.CartItemResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                primaryImageUrl,
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity(),
                cartItem.getSubTotal()
        );
    }
}
