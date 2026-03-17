package com.techgadget.ecommerce.unit_test;

import com.techgadget.ecommerce.dto.request.cart.AddCartItemRequest;
import com.techgadget.ecommerce.dto.request.cart.UpdateCartItemRequest;
import com.techgadget.ecommerce.dto.response.cart.CartResponse;
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
import com.techgadget.ecommerce.service.CartService;
import com.techgadget.ecommerce.service.ProductImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @InjectMocks
    private CartService cartService;

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageService productImageService;

    // Shared test data
    private User user;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User("username", "email@gmail.com", "hashed", "full name");
        ReflectionTestUtils.setField(user, "id", 1L);

        product = new Product();
        ReflectionTestUtils.setField(product, "id", 1L);
        product.setName("iPhone 15");
        product.setPrice(15_000_000L);
        product.setStock(5);

        // Existing empty cart, ready to be filled
        cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", 1L);
    }

    @Nested
    @DisplayName("addToCart()")
    class AddToCart {

        private AddCartItemRequest addCartItemRequest;
        private CartItem cartItem;

        @BeforeEach
        void setUp() {
            addCartItemRequest = new AddCartItemRequest(1L, 2);

            cartItem = new CartItem(cart, product, 2);
            ReflectionTestUtils.setField(cartItem, "id", 1L);
        }

        @Test
        @DisplayName("new item - new CartItem added to cart")
        void newItem_successAddToCart() {
            // Cart already exists
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            // Cart item doesn't exists yet
            when(cartItemRepository.findByCart_IdAndProduct_IdWithProduct(1L, 1L))
                    .thenReturn(Optional.empty());
            when(cartRepository.save(cart)).thenReturn(cart);

            CartResponse response = cartService.addToCart(1L, addCartItemRequest);

            assertThat(response).isNotNull();
            // Verify size and quantity - 1 new item with quantity 2
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().getFirst().getQuantity()).isEqualTo(2L);

            verify(cartRepository, times(1)).save(cart);
        }

        @Test
        @DisplayName("existing item - quantity incremented, no new CartItem created")
        void existingItem_quantityIncremented() {

            // Cart has 1 item
            cart.addItem(cartItem);

            // Cart already exists
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            // Cart item already exists
            when(cartItemRepository.findByCart_IdAndProduct_IdWithProduct(1L, 1L))
                    .thenReturn(Optional.of(cartItem));
            when(cartRepository.save(cart)).thenReturn(cart);

            CartResponse response = cartService.addToCart(1L, addCartItemRequest);

            assertThat(response).isNotNull();
            // Size must be 1, quantity must be 2 + 2 = 4
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().getFirst().getQuantity()).isEqualTo(4L);

            verify(cartRepository, times(1)).save(cart);
        }

        @Test
        void productNotFound_throwsNotFoundException() {

            // Cart already exists
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            // Product not found, returns empty
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(1L, addCartItemRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Product not found.");

            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("new item, insufficient stock - throws ConflictException")
        void newItem_insufficientStock_throwsConflictException() {

            // Product stock is 0, but requested quantity is 2
            product.setStock(0);

            // Cart already exists
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            // New item, create new CartItem
            when(cartItemRepository.findByCart_IdAndProduct_IdWithProduct(1L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(1L, addCartItemRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Stock quantity not sufficient.");

            verify(cartRepository, never()).save(any());
        }

        @Test
        void existingItem_insufficientStock_throwsConflictException() {

            // Product stock is 0, but requested quantity is 4
            product.setStock(0);
            // Cart has 1 item
            cart.addItem(cartItem);

            // Cart already exists
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            // New item, create new CartItem
            when(cartItemRepository.findByCart_IdAndProduct_IdWithProduct(1L, 1L))
                    .thenReturn(Optional.of(cartItem));

            assertThatThrownBy(() -> cartService.addToCart(1L, addCartItemRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Stock quantity not sufficient.");

            verify(cartRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCart()")
    class GetCart {

        @Test
        @DisplayName("cart exists - returns existing cart")
        void cartExists_returnsCart() {

            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));

            CartResponse response = cartService.getCart(1L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(0);

            verify(cartRepository, never()).save(cart);
        }

        @Test
        @DisplayName("cart not exists - creates and returns new cart")
        void cartNotExists_createNewCart_success() {

            // Returns empty
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            CartResponse response = cartService.getCart(1L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(0);

            verify(cartRepository, times(1)).save(any());
        }
    }

    @Nested
    @DisplayName("updateCartItem()")
    class UpdateCartItem {

        private CartItem cartItem;

        @BeforeEach
        void setUp() {
            cartItem = new CartItem(cart, product, 2);
            ReflectionTestUtils.setField(cartItem, "id", 1L);
        }

        @Test
        @DisplayName("success - update CartItem quantity")
        void success_updateItemQuantity() {

            // Update quantity to 1
            UpdateCartItemRequest request = new UpdateCartItemRequest(1);

            // Cart has 1 item with id 1L
            cart.addItem(cartItem);

            when(cartItemRepository.findByIdAndCart_User_IdWithProduct(1L, 1L))
                    .thenReturn(Optional.of(cartItem));
            // Cart exists
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));


            CartResponse response = cartService.updateCartItem(1L, 1L, request);

            assertThat(response).isNotNull();
            // Size must be 1, quantity must be updated to 10
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().getFirst().getQuantity()).isEqualTo(1);

            verify(cartItemRepository, times(1)).save(cartItem);
        }

        @Test
        @DisplayName("cart item not found - throws NotFoundException")
        void itemNotFound_throwsNotFoundException() {

            // Update quantity to 10
            UpdateCartItemRequest request = new UpdateCartItemRequest(10);

            // Cart has 1 item with id 1L
            cart.addItem(cartItem);

            when(cartItemRepository.findByIdAndCart_User_IdWithProduct(1L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateCartItem(1L, 1L, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Cart item not found.");
        }
    }

    @Nested
    @DisplayName("removeCartItem()")
    class RemoveCartItem {

        @Test
        @DisplayName("success - remove cart item from cart")
        void success_removeItem() {

            // Cart has 1 item
            CartItem cartItem = new CartItem(cart, product, 2);
            ReflectionTestUtils.setField(cartItem, "id", 1L);
            cart.addItem(cartItem);

            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));

            CartResponse response = cartService.removeCartItem(1L, 1L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(0);

            verify(cartRepository, times(1)).save(cart);
        }

        @Test
        @DisplayName("CartItem not found in cart - throws NotFoundException")
        void itemNotFound_throwsNotFoundException() {

            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));

            assertThatThrownBy(() -> cartService.removeCartItem(1L, 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Cart item not found.");

            verify(cartRepository, never()).save(cart);
        }
    }

    @Nested
    @DisplayName("clearCart()")
    class ClearCart {

        @BeforeEach
        void setUp() {
            // Add 1 item to cart
            CartItem cartItem = new CartItem(cart, product, 2);
            ReflectionTestUtils.setField(cartItem, "id", 1L);

            cart.addItem(cartItem);
        }

        @Test
        @DisplayName("success - clear all CartItem in cart")
        void success() {

            assertThat(cart.getItems()).hasSize(1);

            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));

            CartResponse response = cartService.clearCart(1L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(0);

            verify(cartRepository, times(1)).save(cart);
        }
    }
}