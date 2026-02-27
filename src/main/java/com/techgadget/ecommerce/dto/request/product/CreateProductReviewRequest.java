package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductReviewRequest {

    @NotNull(message = "Existing order is required.")
    private Long orderId;

    @NotNull(message = "Rating is required.")
    @Min(value = 0, message = "Rating minimum value is 0.")
    @Max(value = 5, message = "Rating maximum value is 5.")
    private Integer rating;

    @NotNull(message = "Comment is required.")
    @Size(max = 500, message = "Comment maximum length is 500.")
    private String comment;

}
