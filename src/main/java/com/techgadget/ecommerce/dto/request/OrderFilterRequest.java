package com.techgadget.ecommerce.dto.request;

import com.techgadget.ecommerce.domain.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for getting orders with query param
 * -
 * Make sure using @ModelAttribute
 */
@Data
@NoArgsConstructor
public class OrderFilterRequest {

    @Nullable
    private String status;
    @Nullable
    private LocalDate fromDate;
    @Nullable
    private LocalDate toDate;

    @Min(value = 0, message = "Page minimum value is 0.")
    @NotNull(message = "Page is required.")
    private Integer page = 0;

    @Min(value = 0, message = "Size minimum value is 0.")
    @NotNull(message = "Size is required.")
    private Integer size = 10;

    @NotNull(message = "Sort is required.")
    private String sort = "NEWEST";
}
