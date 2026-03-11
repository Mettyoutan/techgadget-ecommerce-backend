package com.techgadget.ecommerce.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private String description;
    private Long price;
    private Integer stock;

    // Only need primary image
    private String imageUrl;

    private Map<String, Object> specs;

    // Nested category DTO
    private CategoryResponse category;

}
