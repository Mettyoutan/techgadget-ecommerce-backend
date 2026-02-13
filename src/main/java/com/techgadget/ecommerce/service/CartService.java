    package com.techgadget.ecommerce.service;

    import com.techgadget.ecommerce.dto.request.AddCartItemRequest;
    import com.techgadget.ecommerce.dto.request.UpdateCartItemRequest;
    import com.techgadget.ecommerce.dto.response.CartResponse;
    import com.techgadget.ecommerce.entity.Cart;
    import com.techgadget.ecommerce.entity.CartItem;
    import com.techgadget.ecommerce.entity.Product;
    import com.techgadget.ecommerce.entity.User;
    import com.techgadget.ecommerce.exception.ConflictException;
    import com.techgadget.ecommerce.exception.NotFoundException;
    import com.techgadget.ecommerce.repository.CartItemRepository;
    import com.techgadget.ecommerce.repository.CartRepository;
    import com.techgadget.ecommerce.repository.ProductRepository;
    import com.techgadget.ecommerce.repository.UserRepository;
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

        /**
         * Helper method to get cart entity
         * If not exists (null), create new cart
         */
        private Cart getOrCreateCartEntity(Long userId) {
            log.debug("Processing get or create cart entity - User: {}", userId);

            // Get cart with OPTIMIZED QUERY --> JOIN FETCH items
            Cart cart = cartRepository.findByUser_IdWithItems(userId).orElse(null);

            if (cart == null) {
                log.info("Cart not found for user id = {}.", userId);
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> {
                            log.warn("User not found with id = {}.", userId);
                            return new NotFoundException("User not found.");
                        });

                // Create new cart for user
                cart = cartRepository.save(new Cart(user));
            }

            log.debug("Cart retrieved/created - Cart: {}", cart.getId());

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
            log.debug("Processing add to cart request - User: {}, Product: {}, Quantity: {}",
                    userId, request.getProductId(), request.getQuantity());

            // Get or create cart
            Cart cart = getOrCreateCartEntity(userId);

            // Get product & check stock
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> {
                        log.warn("Product not found with id = {}.", request.getProductId());
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
                cartItem.setQuantity(newQuantity);

                int index = cart.getItems().indexOf(cartItem);
                if (index >= 0) {
                    cart.getItems().set(index, cartItem);  // Force update reference
                }

                log.debug("Updated existing cart item - CartItem: {}, Product: {}, Quantity: {} -> {}",
                        cartItem.getId(), product.getId(), oldQuantity, newQuantity);

            } else {
                // Check if product stock not sufficient to quantity
                if (!product.isQuantitySufficient(request.getQuantity())) {
                    throw new ConflictException("Quantity not sufficient.");
                }

                // Create new cart item & add to cart
                cartItem = new CartItem(cart, product, request.getQuantity());
                cart.addItem(cartItem);

                log.debug("Added new cart item - CartItem: {}, Product: {}, Quantity: {}",
                        cartItem.getId(), product.getId(), request.getQuantity());
            }

            // Save cart
            cartRepository.save(cart);

            log.info("User {} successfully added product {} to cart {} - Quantity: {}, " +
                            "Cart Total Items: {}, Cart Value: Rp {}",
                    userId,
                    request.getProductId(),
                    cart.getId(),
                    request.getQuantity(),
                    cart.getItems().size(),
                    cart.getTotalItems());

            return mapToCartResponse(cart);
        }

        /**
         * Update cart item (only quantity)
         */
        @Transactional
        public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
            log.debug("Processing update cart item - User: {}, CartItem: {}, Quantity: {}",
                    userId, cartItemId, request.getQuantity());

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

            log.info("User {} successfully updated cart item {} in cart {} - Quantity: {}",
                    userId,
                    cartItemId,
                    cart.getId(),
                    request.getQuantity());

            return mapToCartResponse(cart);
        }

        /**
         * Remove cart item
         */
        @Transactional
        public CartResponse removeCartItem(Long userId, Long cartItemId) {
            log.debug("Processing remove cart item - User: {}, CartItem: {}",
                    userId, cartItemId);

            Cart cart = getOrCreateCartEntity(userId);

            // Find cart item with id
            CartItem cartItem = cart.getItems()
                    .stream()
                    .filter(ci -> ci.getId().equals(cartItemId)).findFirst()
                    .orElseThrow(() -> {
                        log.warn("Cart item not found with id = {} - user id = {}.", cartItemId, userId);
                        return new NotFoundException("Cart item not found.");
                    });

            // remove and save
            cart.getItems().remove(cartItem);
            cartRepository.save(cart);

            log.info("User {} successfully removed cart item {} in cart {}",
                    userId, cartItemId, cart.getId());

            return mapToCartResponse(cart);
        }

        @Transactional
        public CartResponse clearCart(Long userId) {
            log.debug("Processing clear cart - User: {}", userId);

            Cart cart = getOrCreateCartEntity(userId);

            cart.getItems().clear();
            cartRepository.save(cart);

            log.info("User {} successfully cleared cart {}",
                    userId, cart.getId());

            return mapToCartResponse(cart);
        }

        @Transactional(readOnly = true)
        public CartResponse.CartCountResponse getCartItemCount(Long userId) {
            log.debug("Processing get cart item count - User: {}", userId);

            Cart cart = getOrCreateCartEntity(userId);

            log.info("User {} got total {} cart items on cart {}",
                    userId, cart.getTotalItems(), cart.getId());

            return new CartResponse.CartCountResponse(cart.getTotalItems());
        }

        private CartResponse mapToCartResponse(Cart cart) {

            // Create cart response
            CartResponse cartResponse = new CartResponse();
            cartResponse.setTotalItems(cart.getTotalItems());
            cartResponse.setTotalPrice(cart.getTotalPrice());

            // Create cart item response
            List<CartResponse.CartItemResponse> items = new ArrayList<>();
            for (CartItem cartItem : cart.getItems()) {
                CartResponse.CartItemResponse itemResponse = mapToCartItemResponse(cartItem);

                items.add(itemResponse);
            }
            cartResponse.setItems(items);

            return cartResponse;
        }

        private CartResponse.CartItemResponse mapToCartItemResponse(CartItem cartItem) {
            CartResponse.CartItemResponse itemResponse = new CartResponse.CartItemResponse();
            itemResponse.setId(cartItem.getId());
            itemResponse.setProductId(cartItem.getProduct().getId());
            itemResponse.setProductName(cartItem.getProduct().getName());
            itemResponse.setImageUrl(cartItem.getProduct().getImageUrl());
            itemResponse.setQuantity(cartItem.getQuantity());
            itemResponse.setPriceInRupiah(cartItem.getProduct().getPriceInRupiah());
            itemResponse.setSubtotal(cartItem.getSubTotal());
            return itemResponse;
        }
    }
