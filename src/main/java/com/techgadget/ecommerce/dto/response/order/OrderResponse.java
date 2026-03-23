package com.techgadget.ecommerce.dto.response.order;

import com.techgadget.ecommerce.dto.response.image.ImageResponse;
import com.techgadget.ecommerce.dto.response.user.AddressResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private String orderStatus;
    private String paymentStatus;

    private Long totalPrice;
    private Integer totalItems;

    private LocalDateTime createdAt;

    private AddressResponse shippingAddress;

    private List<OrderItemResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
    
        private Long id;
        private Long productId;
        private String productName;
        private String imageUrl;

        private Integer quantity;
        private Long priceAtOrder;
        private Long subtotal;
    }
}
