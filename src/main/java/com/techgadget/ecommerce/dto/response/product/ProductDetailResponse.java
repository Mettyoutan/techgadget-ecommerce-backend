package com.techgadget.ecommerce.dto.response.product;

import com.techgadget.ecommerce.dto.response.image.ImageResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
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
    private Long price;
    private Integer stock;

    // List of images for product detail
    private List<ImageResponse> images;

    private Map<String, Object> specs;

    // Nested category DTO
    private CategoryResponse category;
}
