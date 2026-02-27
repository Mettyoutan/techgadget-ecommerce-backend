package com.techgadget.ecommerce.dto.response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private List<CartItemResponse> items;
    private Long totalPrice;
    private Integer totalItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String imageUrl;
        private Long priceInRupiah;
        private Integer quantity;
        private Long subtotal; // price * quantity
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartCountResponse {
        private Integer count;
    }
}
