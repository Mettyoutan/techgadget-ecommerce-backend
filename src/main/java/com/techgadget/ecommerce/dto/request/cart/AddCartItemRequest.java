package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request to create cart items and add it to cart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequest {

    @NotNull(message = "Product ID required.")
    private Long productId;

    @NotNull(message = "Quantity required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity;
}
