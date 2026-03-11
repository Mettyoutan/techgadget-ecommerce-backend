package com.techgadget.ecommerce.dto.response.auth;

import com.techgadget.ecommerce.dto.response.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Used for AuthService response to AuthController
 */
@Getter
@AllArgsConstructor
public class AuthServiceResponse {

    private String message;
    private String access;
    private String refresh;
    private UserResponse user;
}
