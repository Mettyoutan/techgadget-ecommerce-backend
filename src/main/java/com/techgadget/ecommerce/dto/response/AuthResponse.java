package com.techgadget.ecommerce.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private String token;
    private UserResponse user;

}
