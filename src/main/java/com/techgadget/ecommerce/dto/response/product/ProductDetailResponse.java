package com.techgadget.ecommerce.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String description;
    private Long priceInRupiah;
    private Integer stockQuantity;

    // List of images for product detail
    private List<String> images;

    private Map<String, Object> specs;

    // Nested category DTO
    private CategoryResponse category;
}
