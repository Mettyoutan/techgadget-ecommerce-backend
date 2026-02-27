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
public class RegisterRequest {

    @NotBlank(message = "Username required.")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters.")
    private String username;

    @NotBlank(message = "Email required.")
    @Email(message = "Invalid email format.")
    private String email;

    @NotBlank(message = "Password required.")
    @Size(min = 6, max = 100, message = "Password must be 6-100 characters.")
    private String password;

    @NotBlank(message = "Full name required.")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters.")
    private String fullName;
}
