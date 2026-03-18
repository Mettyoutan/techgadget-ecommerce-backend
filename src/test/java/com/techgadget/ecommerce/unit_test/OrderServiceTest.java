package com.techgadget.ecommerce.unit_test;

import com.techgadget.ecommerce.dto.request.order.CreateOrderRequest;
import com.techgadget.ecommerce.dto.request.order.OrderFilterRequest;
import com.techgadget.ecommerce.dto.request.order.UpdateOrderStatusToShipRequest;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.order.OrderResponse;
import com.techgadget.ecommerce.entity.*;
import com.techgadget.ecommerce.enums.OrderStatus;
import com.techgadget.ecommerce.enums.PaymentMethod;
import com.techgadget.ecommerce.enums.PaymentStatus;
import com.techgadget.ecommerce.exception.BadRequestException;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.*;
import com.techgadget.ecommerce.service.OrderService;
import com.techgadget.ecommerce.service.ProductImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductImageService productImageService;

    // Shared test data
    private User user;
    private Address shippingAddress;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private Order pendingOrder;
    private OrderItem orderItem;
    private Payment pendingPayment;

    @BeforeEach
    public void setup() {
        Category category = new Category(1L, "Electronics", "");
        ReflectionTestUtils.setField(category, "id", 1L);

        user = new User("username", "email@gmail.com", "hashed", "full name", "12345");
        ReflectionTestUtils.setField(user, "id", 1L);

        shippingAddress = new Address(
                user,
                "username",
                "12345",
                "Sudirman",
                "South Jakarta",
                "Dki Jakarta",
                "postal code",
                "",
                true
        );
        ReflectionTestUtils.setField(shippingAddress, "id", 1L);

        product = new Product(category, "Iphone 14", "", 15_000_000L, 5, Map.of());
        ReflectionTestUtils.setField(product, "id", 1L);

        cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", 1L);

        cartItem = new CartItem(cart, product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 1L);

        // Add 1 item to cart
        cart.addItem(cartItem);

        // Create pending order with 1 orderItem
        pendingOrder = new Order(user, "ORD-1", shippingAddress);
        ReflectionTestUtils.setField(pendingOrder, "id", 1L);

        orderItem = new OrderItem(
                pendingOrder,
                1L,
                "Iphone 14",
                null,
                2,
                15_000_000L
        );
        ReflectionTestUtils.setField(orderItem, "id", 1L);

        pendingOrder.addItem(orderItem);

        // Add pending payment to order
        pendingPayment = new Payment(
                pendingOrder,
                15_000_000L,
                PaymentStatus.PENDING,
                PaymentMethod.DUMMY,
                null
        );
        ReflectionTestUtils.setField(pendingPayment, "id", 1L);

        pendingOrder.setPayment(pendingPayment);
    }

    @Nested
    @DisplayName("createOrder()")
    class CreateOrder {

        // Helper method: build CreateOrderRequest
        private CreateOrderRequest buildCreateOrderRequest(List<Long> cartItemIds) {
            return new CreateOrderRequest(
                    cartItemIds, 1L, "DUMMY");
        }

        @Test
        @DisplayName("success - order created with snapshot data")
        void success_orderCreatedWithSnapshotData() {

            CreateOrderRequest request = buildCreateOrderRequest(List.of(1L));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            when(addressRepository.findByIdAndUser_Id(1L, 1L)).thenReturn(Optional.of(shippingAddress));
            when(paymentRepository.save(any(Payment.class)))
                    .thenReturn(pendingPayment);
            when(orderRepository.save(any(Order.class)))
                    .thenReturn(pendingOrder);
            // order.getId() is populated after save() returns pendingOrder (id=1L)
            when(orderRepository.findUserOrderById(eq(1L), eq(1L)))
                    .thenReturn(Optional.of(pendingOrder));

            OrderResponse response = orderService.createOrder(1L, request);

            assertThat(response).isNotNull();
            // Order and Payment status must be PENDING
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING.toString());
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING.toString());

            // Product stock must be decremented from 5 to 3
            assertThat(product.getStock()).isEqualTo(3);

            verify(orderRepository, atLeastOnce()).save(any(Order.class));
            verify(paymentRepository, times(1)).save(any(Payment.class));
        }

        @Test
        @DisplayName("cart is empty - throws ConflictException")
        void cartIsEmpty_throwsConflictException() {

            // Cart has no items
            cart.getItems().clear();

            CreateOrderRequest request = buildCreateOrderRequest(List.of(1L));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Cart is empty.");
        }

        @Test
        @DisplayName("address for shipping does not exist - throws NotFoundException")
        void addressNotExists_throwsNotFoundException() {

            // Requesting address with id 10, which does not exist
            CreateOrderRequest request = buildCreateOrderRequest(List.of(1L));
            request.setAddressId(10L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            // Address not found with id
            when(addressRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Address not found.");
        }

        @Test
        @DisplayName("cart item id not in cart - throws NotFoundException")
        void cartItemNotInCart_throwsNotFoundException() {

            // Requesting CartItem with id 99, which does not exist in cart
            CreateOrderRequest request = buildCreateOrderRequest(List.of(99L));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            when(addressRepository.findByIdAndUser_Id(1L, 1L)).thenReturn(Optional.of(shippingAddress));

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Cart item not found.");
        }

        @Test
        @DisplayName("insufficient product stock - throws ConflictException")
        void insufficientStock_throwsConflictException() {

            // Product stock is 1, cart item quantity is 2
            product.setStock(1);

            CreateOrderRequest request = buildCreateOrderRequest(List.of(1L));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            when(addressRepository.findByIdAndUser_Id(1L, 1L)).thenReturn(Optional.of(shippingAddress));

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Product quantity insufficient.");
        }

        @Test
        @DisplayName("invalid payment method - throws BadRequestException")
        void invalidPaymentMethod_throwsBadRequestException() {

            CreateOrderRequest request = buildCreateOrderRequest(List.of(1L));
            request.setPaymentMethod("INVALID_METHOD");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUser_IdWithItems(1L)).thenReturn(Optional.of(cart));
            when(addressRepository.findByIdAndUser_Id(1L, 1L)).thenReturn(Optional.of(shippingAddress));

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid payment method: INVALID_METHOD");
        }
    }

    @Nested
    @DisplayName("cancelOrder()")
    class CancelOrder {

        @Test
        @DisplayName("success - order cancelled and stock restored")
        void success_orderCancelledAndStockRestored() {

            // Product stock after order is 3, when cancelled, stock restored to 5
            product.setStock(3);

            when(orderRepository.findUserOrderById(1L, 1L)).thenReturn(Optional.of(pendingOrder));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            OrderResponse response = orderService.cancelOrder(1L, 1L);

            assertThat(response).isNotNull();

            // Order status must be cancelled, payment status must be failed
            assertThat(pendingOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(pendingOrder.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

            // Product stock must be 5 (3 + 2)
            assertThat(product.getStock()).isEqualTo(5);

            verify(productRepository, times(1)).save(any(Product.class));
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("order not found - throws NotFoundException")
        void orderNotFound_throwsNotFoundException() {

            when(orderRepository.findUserOrderById(1L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("order status is not PENDING - throws ConflictException")
        void orderStatusNotPending_throwsConflictException() {

            // Order is already CONFIRMED — cannot be cancelled via customer endpoint
            pendingOrder.setOrderStatus(OrderStatus.CONFIRMED);

            when(orderRepository.findUserOrderById(1L, 1L)).thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Confirmed order cannot be cancelled.");

            verify(productRepository, never()).save(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("product id snapshot not found in DB - throws NotFoundException")
        void productIdSnapshotNotFound_throwsNotFoundException() {

            // Order item references product id 10, which no longer exists in DB
            pendingOrder.getItems().getFirst().setProductIdSnapshot(10L);

            when(orderRepository.findUserOrderById(1L, 1L)).thenReturn(Optional.of(pendingOrder));
            when(productRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Product id snapshot not found.");

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("getUserOrders()")
    class GetUserOrders {

        private OrderFilterRequest buildFilter(String status) {
            OrderFilterRequest filter = new OrderFilterRequest();
            filter.setStatus(status);
            filter.setPage(0);
            filter.setSize(10);
            return filter;
        }

        @Test
        @DisplayName("invalid status filter - throws BadRequestException")
        void invalidStatusFilter_throwsBadRequestException() {

            OrderFilterRequest filter = buildFilter("INVALID_STATUS");

            assertThatThrownBy(() -> orderService.getUserOrders(1L, filter))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid order status.");
        }

        @Test
        @DisplayName("no filter - calls findUserOrders with null status")
        void noFilter_callsFindUserOrders() {

            OrderFilterRequest filter = buildFilter(null);
            Page<Order> page = new PageImpl<>(List.of(pendingOrder));

            when(orderRepository.findUserOrders(eq(1L), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            PaginatedResponse<OrderResponse> response = orderService.getUserOrders(1L, filter);

            assertThat(response).isNotNull();
            assertThat(response.getTotalElements()).isEqualTo(1L);

            verify(orderRepository, times(1))
                    .findUserOrders(eq(1L), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("with valid status - passes parsed OrderStatus to repository")
        void withValidStatus_passesOrderStatusToRepository() {

            OrderFilterRequest filter = buildFilter("PENDING");
            Page<Order> page = new PageImpl<>(List.of(pendingOrder));

            when(orderRepository.findUserOrders(eq(1L), eq(OrderStatus.PENDING), any(Pageable.class)))
                    .thenReturn(page);

            PaginatedResponse<OrderResponse> response = orderService.getUserOrders(1L, filter);

            assertThat(response.getTotalElements()).isEqualTo(1L);
            verify(orderRepository, times(1))
                    .findUserOrders(eq(1L), eq(OrderStatus.PENDING), any(Pageable.class));
        }

        @Test
        @DisplayName("with fromDate only - calls findUserOrdersFromDate")
        void withFromDateOnly_callsFromDateRepository() {

            OrderFilterRequest filter = buildFilter(null);
            filter.setFromDate(LocalDate.of(2025, 1, 1));
            Page<Order> page = new PageImpl<>(List.of(pendingOrder));

            when(orderRepository.findUserOrdersFromDate(
                    eq(1L), isNull(), any(), any(Pageable.class)))
                    .thenReturn(page);

            PaginatedResponse<OrderResponse> response = orderService.getUserOrders(1L, filter);

            assertThat(response.getTotalElements()).isEqualTo(1L);
            verify(orderRepository, times(1))
                    .findUserOrdersFromDate(eq(1L), isNull(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("with toDate only - calls findUserOrdersToDate")
        void withToDateOnly_callsToDateRepository() {

            OrderFilterRequest filter = buildFilter(null);
            filter.setToDate(LocalDate.of(2025, 12, 31));
            Page<Order> page = new PageImpl<>(List.of(pendingOrder));

            when(orderRepository.findUserOrdersToDate(
                    eq(1L), isNull(), any(), any(Pageable.class)))
                    .thenReturn(page);

            PaginatedResponse<OrderResponse> response = orderService.getUserOrders(1L, filter);

            assertThat(response.getTotalElements()).isEqualTo(1L);
            verify(orderRepository, times(1))
                    .findUserOrdersToDate(eq(1L), isNull(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("with fromDate and toDate - calls findUserOrdersBetweenDate")
        void withBothDates_callsBetweenDateRepository() {

            OrderFilterRequest filter = buildFilter(null);
            filter.setFromDate(LocalDate.of(2025, 1, 1));
            filter.setToDate(LocalDate.of(2025, 12, 31));
            Page<Order> page = new PageImpl<>(List.of(pendingOrder));

            when(orderRepository.findUserOrdersBetweenDate(
                    eq(1L), isNull(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            PaginatedResponse<OrderResponse> response = orderService.getUserOrders(1L, filter);

            assertThat(response.getTotalElements()).isEqualTo(1L);
            verify(orderRepository, times(1))
                    .findUserOrdersBetweenDate(eq(1L), isNull(), any(), any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("adminUpdateOrderStatusToShip()")
    class AdminUpdateOrderStatusToShip {

        private UpdateOrderStatusToShipRequest buildShipRequest() {
            return new UpdateOrderStatusToShipRequest("JNE", "JNE123456");
        }

        @Test
        @DisplayName("success - CONFIRMED order shipped with tracking info")
        void success_confirmedOrderShipped() {

            pendingOrder.setOrderStatus(OrderStatus.CONFIRMED);

            when(orderRepository.findOrderByIdWithRelationForAdmin(1L))
                    .thenReturn(Optional.of(pendingOrder));

            OrderResponse response = orderService.adminUpdateOrderStatusToShip(1L, buildShipRequest());

            assertThat(response).isNotNull();
            assertThat(pendingOrder.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
            assertThat(pendingOrder.getShippingProvider()).isEqualTo("JNE");
            assertThat(pendingOrder.getTrackingNumber()).isEqualTo("JNE123456");

            verify(orderRepository, times(1)).save(pendingOrder);
        }

        @Test
        @DisplayName("invalid transition - PENDING cannot transition to SHIPPED")
        void invalidTransition_throwsConflictException() {

            // PENDING → SHIPPED is not a valid transition
            assertThat(pendingOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

            when(orderRepository.findOrderByIdWithRelationForAdmin(1L))
                    .thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> orderService.adminUpdateOrderStatusToShip(1L, buildShipRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Cannot change order status");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("order not found - throws NotFoundException")
        void orderNotFound_throwsNotFoundException() {

            when(orderRepository.findOrderByIdWithRelationForAdmin(1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.adminUpdateOrderStatusToShip(1L, buildShipRequest()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found.");
        }
    }

    @Nested
    @DisplayName("adminUpdateOrderStatusToComplete()")
    class AdminUpdateOrderStatusToComplete {

        @Test
        @DisplayName("success - SHIPPED order completed")
        void success_shippedOrderCompleted() {

            pendingOrder.setOrderStatus(OrderStatus.SHIPPED);

            when(orderRepository.findOrderByIdWithRelationForAdmin(1L))
                    .thenReturn(Optional.of(pendingOrder));

            orderService.adminUpdateOrderStatusToComplete(1L);

            assertThat(pendingOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
            verify(orderRepository, times(1)).save(pendingOrder);
        }

        @Test
        @DisplayName("invalid transition - COMPLETED cannot transition further")
        void invalidTransition_throwsConflictException() {

            pendingOrder.setOrderStatus(OrderStatus.COMPLETED);

            when(orderRepository.findOrderByIdWithRelationForAdmin(1L))
                    .thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> orderService.adminUpdateOrderStatusToComplete(1L))
                    .isInstanceOf(ConflictException.class);

            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("adminUpdateOrderStatusToCancel()")
    class AdminUpdateOrderStatusToCancel {

        @Test
        @DisplayName("success - CONFIRMED order cancelled by admin")
        void success_confirmedOrderCancelled() {

            pendingOrder.setOrderStatus(OrderStatus.CONFIRMED);

            when(orderRepository.findOrderByIdWithRelationForAdmin(1L))
                    .thenReturn(Optional.of(pendingOrder));

            orderService.adminUpdateOrderStatusToCancel(1L);

            assertThat(pendingOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository, times(1)).save(pendingOrder);
        }

        @Test
        @DisplayName("invalid transition - CANCELLED cannot transition further")
        void invalidTransition_throwsConflictException() {

            pendingOrder.setOrderStatus(OrderStatus.CANCELLED);

            when(orderRepository.findOrderByIdWithRelationForAdmin(1L))
                    .thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> orderService.adminUpdateOrderStatusToCancel(1L))
                    .isInstanceOf(ConflictException.class);

            verify(orderRepository, never()).save(any());
        }
    }
}