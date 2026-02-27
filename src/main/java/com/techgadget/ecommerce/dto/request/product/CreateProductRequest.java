package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotNull(message = "Category ID required.")
    private Long categoryId;

    @NotBlank(message = "Product name required")
    @Size(max = 200, message = "Name max 200 characters.")
    private String name;

    @Size(max = 2000, message = "Description max 2000 characters.")
    private String description;

    @NotNull(message = "Price required.")
    @Min(value = 1, message = "Price must be greater than 0.")
    @Max(value = 999999999999L, message = "Price too large.")
    private Long priceInRupiah;

    @NotNull(message = "Stock quantity required.")
    @Min(value = 0, message = "Stock cannot be negative.")
    private Integer stockQuantity;

    @Size(max = 500, message = "Image URL max 500 characters.")
    private String imageUrl;

    private Map<String, Object> specs;
}
