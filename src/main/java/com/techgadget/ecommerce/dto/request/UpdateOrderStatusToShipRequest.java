package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusToShipRequest {

    @NotNull(message = "Shipping provider is required.")
    private String shippingProvider;

    @NotNull(message = "Tracking number is required.")
    private String trackingNumber;
}
