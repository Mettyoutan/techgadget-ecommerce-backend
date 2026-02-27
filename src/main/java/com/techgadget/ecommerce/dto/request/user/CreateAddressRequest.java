package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest {

    @NotBlank(message = "Recipient name is required.")
    private String recipientName;

    @NotBlank(message = "Phone number is required.")
    private String phoneNumber;

    @NotBlank(message = "Street is required.")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Province is required.")
    private String province;

    @NotBlank(message = "Postal code is required.")
    private String postalCode;

    private String notes;
}
