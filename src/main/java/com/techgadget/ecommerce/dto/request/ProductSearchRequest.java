package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Sort;

/**
 * DTO for getting products with query param
 * -
 * Make sure using @ModelAttribute
 */
@Data
@NoArgsConstructor
public class ProductSearchRequest {

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

    private Long categoryId;

    @Min(value = 0, message = "Minimum price is 0 rupiah.")
    private Long minPrice = 0L;

    @Max(value = 999_999_999_999L, message = "Maximum price is 999999999999 rupiah.")
    private Long maxPrice = 999_999_999_999L;

    /**
     * Check if price is valid
     */
    @AssertTrue(message = "Minimum price cannot exceed max price.")
    private boolean isPriceValid() {
        return minPrice.compareTo(maxPrice) <= 0;
    }
}
