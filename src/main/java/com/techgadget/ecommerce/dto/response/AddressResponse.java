package com.techgadget.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String recipientName;
    private String phoneNumber;
    private String street;
    private String city;
    private String province;
    private String postalCode;
    private String notes;
}
