package com.techgadget.ecommerce.dto.response.product;

import com.techgadget.ecommerce.dto.response.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProductReviewResponse {

    private Long id;
    private String productName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    private UserResponse user;

}
