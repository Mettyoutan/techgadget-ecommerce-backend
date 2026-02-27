package com.techgadget.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techgadget.ecommerce.entity.Address;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
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
