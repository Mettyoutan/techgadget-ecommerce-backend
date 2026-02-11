package com.techgadget.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewResponse {

    private Long id;
    private Integer rating;
    private String comment;

    private UserResponse user;

    private String productName;
    private LocalDateTime createdAt;

}
