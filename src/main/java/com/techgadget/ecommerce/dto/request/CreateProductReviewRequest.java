package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductReviewRequest {

    @NotNull(message = "Product id is required.")
    private Long productId;

    @NotNull(message = "Rating is required.")
    private Integer rating;

    @NotNull(message = "Comment is required.")
    @Size(min = 1, max = 500)
    private String comment;

}
