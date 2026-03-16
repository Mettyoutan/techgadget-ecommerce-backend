package com.techgadget.ecommerce.dto.request.product;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for getting products with query param
 * -
 * Make sure using @ModelAttribute
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchProductRequest {

    /**
     * Pagination
     */
    @Min(value = 0, message = "Page minimum value is 0.")
    @NotNull(message = "Page is required.")
    private Integer page = 0;

    @Min(value = 1, message = "Size minimum value is 1.")
    @Max(value = 100, message = "Size maximum value is 100.")
    @NotNull(message = "Size is required.")
    private Integer size = 20;

    @NotNull(message = "Sort by is required.")
    private String sortBy = "createdAt";

    @NotNull(message = "Sort dir is required.")
    private String sortDir = "desc";

    /**
     * Search keyword
     */
    private String name = "";

    @Nullable
    private Long categoryId;

    @Nullable
    @Min(value = 0, message = "Minimum price is 0 rupiah.")
    private Long minPrice;

    @Nullable
    @Max(value = 999_999_999_999L, message = "Maximum price is 999999999999 rupiah.")
    private Long maxPrice;

    /**
     * Check if price is valid
     */
    @AssertTrue(message = "Minimum price cannot exceed max price.")
    private boolean isPriceValid() {
        if (minPrice != null && maxPrice != null) {
            return minPrice.compareTo(maxPrice) <= 0;
        }
        return true;
    }


}
