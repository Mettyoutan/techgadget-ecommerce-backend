package com.techgadget.ecommerce.dto.response;

import lombok.*;

/**
 * Used for auth response to client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private String access;
    private UserResponse user;

}
