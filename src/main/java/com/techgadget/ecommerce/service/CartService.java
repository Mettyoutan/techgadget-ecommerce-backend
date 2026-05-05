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

import static net.logstash.logback.argument.StructuredArguments.kv;

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
            log.debug("getOrCreateCart.creating: Cart not found, creating new.");
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("getOrCreateCart.failed: User not found.");
                        return new NotFoundException("User not found.");
                    });

            // Create new cart for user
            cart = cartRepository.save(new Cart(user));
        }

        log.debug("Cart retrieved/created. [{}]",
                kv("cartId", cart.getId()));

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
        log.debug("addToCart.started.",
                kv("productId", request.getProductId()),
                kv("quantity", request.getQuantity())
        );

        // Get or create cart
        Cart cart = getOrCreateCartEntity(userId);

        // Get product & check stock
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> {
                    log.warn("addToCart.failed: product not found.",
                            kv("productId", request.getProductId())
                    );
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
                log.warn("addToCart.failed: insufficient stock.",
                        kv("productId", product.getId()),
                        kv("requested", newQuantity),
                        kv("stock", product.getStock())
                );
                throw new ConflictException("Stock quantity not sufficient.");
            }

            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);

//                int index = cart.getItems().indexOf(cartItem);
//                if (index >= 0) {
//                    cart.getItems().set(index, cartItem);  // Force update reference
//                }

            log.debug("addToCart.quantityUpdated.",
                    kv("cartId", cart.getId()),
                    kv("cartItemId", cartItem.getId()),
                    kv("productId", product.getId()),
                    kv("oldQuantity", oldQuantity),
                    kv("newQuantity", newQuantity)
            );

        } else {
            // Check if product stock not sufficient to quantity
            if (!product.isStockSufficient(request.getQuantity())) {
                log.warn("addToCart.failed: insufficient stock.",
                        kv("productId", product.getId()),
                        kv("requested", request.getQuantity()),
                        kv("stock", product.getStock())
                );
                throw new ConflictException("Stock quantity not sufficient.");
            }

            // Create new cart item & add to cart
            cartItem = new CartItem(cart, product, request.getQuantity());
            cart.addItem(cartItem);

            log.debug("addToCart.success.",
                    kv("cartId", cart.getId()),
                    kv("cartItemId", cartItem.getId()),
                    kv("productId", product.getId()),
                    kv("quantity", request.getQuantity()),
                    kv("totalItems", cart.getItems().size())
            );
        }

        // Save cart
        cart = cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    /**
     * Update cart item (only quantity for now)
     */
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        log.debug("updateCartItem.started.",
                kv("cartItemId", cartItemId),
                kv("newQuantity", request.getQuantity())
        );

        // Get cart item using id and user id
        CartItem cartItem = cartItemRepository.findByIdAndCart_User_IdWithProduct(cartItemId, userId)
                .orElseThrow(() -> {
                    log.warn("updateCartItem.failed: cart item not found.",
                            kv("cartItemId", cartItemId)
                    );
                    return new NotFoundException("Cart item not found.");
                });

        // Set new quantity
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        Cart cart = getOrCreateCartEntity(userId);

        log.debug("updateCartItem.success.",
                kv("cartId", cart.getId()),
                kv("cartItemId", cartItemId),
                kv("quantity", request.getQuantity())
        );

        return mapToCartResponse(cart);
    }

    /**
     * Remove cart item
     */
    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        log.debug("removeCartItem.started.",
                kv("cartItemId", cartItemId)
        );

        Cart cart = getOrCreateCartEntity(userId);

        // Find cart item with id
        CartItem cartItem = cart.getItems()
                .stream()
                .filter(ci -> ci.getId().equals(cartItemId)).findFirst()
                .orElseThrow(() -> {
                    log.warn("removeCartItem.failed: cart item not found.",
                            kv("cartItemId", cartItemId)
                    );
                    return new NotFoundException("Cart item not found.");
                });

        // remove and save
        cart.getItems().remove(cartItem);
        cartRepository.save(cart);

        log.debug("removeCartItem.success.",
                kv("cartId", cart.getId()),
                kv("cartItemId", cartItemId)
        );

        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        log.debug("clearCart.started.");

        Cart cart = getOrCreateCartEntity(userId);
        int itemsCleared = cart.getItems().size();

        cart.getItems().clear();
        cartRepository.save(cart);

        log.debug("clearCart.success.",
                kv("cartId", cart.getId()),
                kv("itemsCleared", itemsCleared)
        );

        return mapToCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse.CartCountResponse getCartItemCount(Long userId) {
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
