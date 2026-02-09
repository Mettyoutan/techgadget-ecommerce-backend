package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.entity.Product;
import com.techgadget.ecommerce.repository.OrderRepository;
import com.techgadget.ecommerce.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private List<Product> products = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("product1");
        product1.setCategory(null);
    }

    @Test
    void test_searchOrders_success() {

    }

    @Test
    void searchUserOrders() {
    }

    @Test
    void getOrderById() {
    }

    @Test
    void updateOrderStatus() {
    }
}