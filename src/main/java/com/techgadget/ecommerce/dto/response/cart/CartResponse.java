package com.techgadget.ecommerce.dto.response.cart;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
public class CartResponse {

    private List<CartItemResponse> items;
    private Long totalPrice;
    private Integer totalItems;

    @Getter
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String imageUrl;
        private Long price;
        private Integer quantity;
        private Long subtotal; // price * quantity
    }

    @Getter
    @AllArgsConstructor
    public static class CartCountResponse {
        private Integer count;
    }
}
