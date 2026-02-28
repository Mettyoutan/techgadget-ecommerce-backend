package com.techgadget.ecommerce.dto.request.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "Cart item is required.")
    private List<Long> cartItemIds;

    @NotNull(message = "Address is required.")
    private Long addressId;

    @NotNull(message = "Payment method is required.")
    private String paymentMethod;
}
