package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest {

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
}
