package com.techgadget.ecommerce.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
}
