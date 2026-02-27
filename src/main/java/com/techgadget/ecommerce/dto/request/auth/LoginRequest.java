package com.techgadget.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email required.")
    @Email(message = "Invalid email format.")
    private String email;

    @NotBlank(message = "Password required.")
    @Size(min = 6, max = 100, message = "Password must be 6-100 characters.")
    private String password;
}
