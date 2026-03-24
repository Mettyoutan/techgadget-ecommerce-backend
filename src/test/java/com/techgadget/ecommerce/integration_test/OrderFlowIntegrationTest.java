package com.techgadget.ecommerce.integration_test;

import com.techgadget.ecommerce.dto.request.auth.RegisterRequest;
import com.techgadget.ecommerce.dto.request.cart.AddCartItemRequest;
import com.techgadget.ecommerce.dto.request.order.CreateOrderRequest;
import com.techgadget.ecommerce.dto.response.auth.AuthResponse;
import com.techgadget.ecommerce.dto.response.cart.CartResponse;
import com.techgadget.ecommerce.dto.response.order.OrderResponse;
import com.techgadget.ecommerce.entity.Address;
import com.techgadget.ecommerce.entity.Category;
import com.techgadget.ecommerce.entity.Product;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.enums.OrderStatus;
import com.techgadget.ecommerce.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

/**
// * Integration test for the full order business flow.
// *
 * Things that want to be verified:
 * - Stock is actually decremented in the real DB after order creation
 * - Snapshot data (name, price at time of order) is persisted correctly
 * - Cancel order actually restores stock in the real DB
 * - Checkout with insufficient stock returns 409 at system level
 * - The full multi-step flow (register → cart → order) works end-to-end
 */
@DisplayName("Order Flow Integration Tests")
public class OrderFlowIntegrationTest extends BaseIntegrationTest {

    /*
        For creating an order, needs:
        - product
        - customer token
        -
     */
    private Product product;
    private String customerToken;
    private long addressId;

    @BeforeEach
    void setUp() throws Exception {

        // Persist category & product on DB - get productId
        Category category = new Category("Phone", "");
        categoryRepository.save(category);

        /*
            Product with stock = 20
         */
        product = new Product(category, "Iphone 17", "", 20_000_000L, 20, Map.of());
        productRepository.save(product);

        // Register customer via HTTP, to get a real JWT
        RegisterRequest registerRequest = new RegisterRequest(
                "username", "email@gmail.com", "password", "full name"
        );

        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
                .andExpect(status().isCreated())
                .andReturn();

        // Convert response into AuthResponse - get customer access token
        String jsonResponseBody = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(jsonResponseBody, AuthResponse.class);
        customerToken = response.getAccess();

        // Persist address in DB - get addressId
        User user = userRepository.findByEmail("email@gmail.com").orElseThrow();
        Address address = new Address(
                user, "username", "018364718471", "street", "city", "province", "postalcode", "", true
        );
        addressId = addressRepository.save(address).getId();

    }

    /**
     * Add 1 product to cart.
     * ONLY USE for success scenario (201)
     * @return {@link Long cartItemId}
     */
    private long addProductToCart(int quantity) throws Exception{
        AddCartItemRequest request = new AddCartItemRequest(product.getId(), quantity);

        MvcResult result = mockMvc.perform(post("/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + customerToken)
        )
                .andExpect(status().isCreated())
                .andReturn();
        String jsonResponseBody = result.getResponse().getContentAsString();
        CartResponse response = objectMapper.readValue(jsonResponseBody, CartResponse.class);

        return response.getItems().getFirst().getId();
    }

    /**
     * Create order via HTTP - test full create order flow.
     * ONLY USE for success scenario (201)
     * @return {@link OrderResponse orderResponse}
     */
    private OrderResponse createOrder(long cartItemId) throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(cartItemId), addressId, "DUMMY"
        );

        MvcResult result = mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + customerToken)
        )
                .andExpect(status().isCreated())
                .andReturn();

        String jsonResponseBody = result.getResponse().getContentAsString();

        return objectMapper.readValue(jsonResponseBody, OrderResponse.class);
    }

    /**
     * -> Add product to cart as cartItem -> Add cartItem to order as orderItem
     */
    @Nested
    @DisplayName("POST /orders")
    class CreateOrder {

        @Test
        @DisplayName("success - stock decremented and snapshot persisted in DB, returns correct response")
        void success_stockDecrementedAndSnapshotPersisted_returnsCorrectResponse() throws Exception {

            // Add a product with 2 quantity (stock available)
            long cartItemId = addProductToCart(2);

            // Created order via HTTP
            OrderResponse response = createOrder(cartItemId);

            // Verify OrderResponse structure
            assertThat(response).isNotNull();
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING.toString());
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING.toString());


            assertThat(response.getTotalPrice()).isEqualTo(40_000_000L); // 2 x 20_000_000

            // Verify item in OrderResponse (1 item)
            // Check if snapshot is available
            assertThat(response.getTotalItems()).isEqualTo(1);
            assertThat(response.getItems()).hasSize(1);
            OrderResponse.OrderItemResponse item = response.getItems().getFirst();
            assertThat(item.getProductName()).isEqualTo("Iphone 17");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getPriceAtOrder()).isEqualTo(20_000_000L);
            assertThat(item.getSubtotal()).isEqualTo(40_000_000L);

            // Verify product stock is decremented in DB directly
            int stock = productRepository.findById(product.getId())
                    .orElseThrow()
                    .getStock();
            assertThat(stock).isEqualTo(18); // 20 - 2 = 18
        }

        @Test
        @DisplayName("success - snapshot independent from product changes")
        void success_snapshotIndependentFromProductChanges() throws Exception {

            // Add a product with 2 quantity (stock available)
            long cartItemId = addProductToCart(2);

            // Create order via HTTP
            OrderResponse order = createOrder(cartItemId);

            // Simulate product price change AFTER order was placed
            // Make sure price snapshot doesn't change
            product.setPrice(15_000_000L);
            productRepository.save(product);

            // Fetch order again
            // To make sure AFTER product changes, snapshot didn't change
            MvcResult result = mockMvc.perform(
                    get("/orders/" + order.getId())
                        .header("Authorization", "Bearer " + customerToken)
            )
                    .andExpect(status().isOk())
                    .andReturn();

            OrderResponse fetchedOrder = objectMapper.readValue(
                    result.getResponse().getContentAsString(), OrderResponse.class);

            // Assert priceAtOrder snapshot is still original, not updated by new product price
            assertThat(fetchedOrder.getItems().getFirst().getPriceAtOrder())
                    .isEqualTo(20_000_000L);
        }

        /**
         * Unsuccessfully add item to cart BEFORE create order.
         * Because wanted quantity in insufficient to product stock
         */
        @Test
        @DisplayName("add to cart, but stock insufficient - returns 409, stock unchanged")
        void addToCart_insufficientStock_returns409_stockUnchanged() throws Exception {

            // Add a product with quantity 30 (exceed the product stock).
            // Cart add might return 409, since CartService checks stock too
            AddCartItemRequest request = new AddCartItemRequest(product.getId(), 30);

            mockMvc.perform(post("/cart")
                    .header("Authorization", "Bearer " + customerToken)

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                    // Must be 409
                    .andExpect(status().isConflict());

            Product unchanged = productRepository.findById(product.getId()).orElseThrow();
            assertThat(unchanged.getStock()).isEqualTo(20);
        }

        /**
         * Successfully add item to cart.
         * But failed to build order, because
         * -> Product stock changed AFTER create to cart BEFORE order built
         * Because wanted quantity in insufficient to product stock
         */
        @Test
        @DisplayName("add to cart successful, then build order, but stock insufficient - returns 409, stock unchanged")
        void buildOrder_insufficientStock_returns409_stockUnchanged() throws Exception {

            // Successfully add 1 product to cart, quantity 2 (sufficient)
            long cartItemId = addProductToCart(2);

            // Scenario: product stock changed
            // So stock would be insufficient
            product.setStock(1);
            productRepository.save(product);

            // Create order via HTTP
            // Must returns 409
            CreateOrderRequest orderRequest = new CreateOrderRequest(
                    List.of(cartItemId), addressId, "DUMMY");

            MvcResult result = mockMvc.perform(
                    post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderRequest))

                            .header("Authorization", "Bearer " + customerToken)
            )
                    // Must be 409
                    .andExpect(status().isConflict())
                    .andReturn();

            Product unchanged = productRepository.findById(product.getId()).orElseThrow();
            assertThat(unchanged.getStock()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("POST /orders/{orderId}/cancel")
    class CancelOrder {

        @Test
        @DisplayName("success - order cancelled and stock restored")
        void success_orderCancelledAndStockRestored() throws Exception {

            // First order success with quantity 10, stock sufficient
            long cartItemId = addProductToCart(10);
            OrderResponse response = createOrder(cartItemId);

            // Assert product stock decremented
            // 20 - 10 = 10
            Product afterOrder = productRepository.findById(product.getId()).orElseThrow();
            assertThat(afterOrder.getStock()).isEqualTo(10);

            // Cancel order
            MvcResult result = mockMvc.perform(
                    post("/orders/%s/cancel".formatted(response.getId()))
                            .header("Authorization", "Bearer " + customerToken)
            )
                    .andExpect(status().isOk())
                    .andReturn();

            OrderResponse cancelledOrder = objectMapper.readValue(
                    result.getResponse().getContentAsString(), OrderResponse.class);

            // Assert cancelled order response
            assertThat(cancelledOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED.toString());
            assertThat(cancelledOrder.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED.toString());

            // Verify product stock is restored in DB
            // 10 + 10 = 20 (Before order stock)
            Product afterCancel = productRepository.findById(product.getId()).orElseThrow();
            assertThat(afterCancel.getStock()).isEqualTo(20);
        }

        @Test
        void cancelNonPendingOrder_returns409() throws Exception {

            // First order success with quantity 10, stock sufficient
            long cartItemId = addProductToCart(10);
            OrderResponse order = createOrder(cartItemId);

            // Scenario: order is paid (DUMMY)
            mockMvc.perform(
                    post("/orders/" + order.getId() + "/pay")
                            .header("Authorization", "Bearer " + customerToken)
            )
                    .andExpect(status().isCreated())
                    // Must be pending
                    .andExpect(jsonPath("$.orderStatus").value(OrderStatus.PENDING.toString()))
                    // Must be paid
                    .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PAID.toString()))
                    .andReturn();

            // Cancel paid order (NOT WORKING)
            mockMvc.perform(
                    post("/orders/%s/cancel".formatted(order.getId()))
                            .header("Authorization", "Bearer " + customerToken)
            )
                    // Must be 409
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Paid order cannot be cancelled."));
        }
    }
}
