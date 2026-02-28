package com.techgadget.ecommerce.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private String description;
    private Long priceInRupiah;
    private Integer stockQuantity;

    // Only need primary image
    private String image;

    private Map<String, Object> specs;

    // Nested category DTO
    private CategoryResponse category;

}
