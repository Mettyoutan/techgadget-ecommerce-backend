package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.LoginRequest;
import com.techgadget.ecommerce.dto.request.RegisterRequest;
import com.techgadget.ecommerce.dto.response.AuthResponse;
import com.techgadget.ecommerce.exception.BadRequestException;
import com.techgadget.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register (PUBLIC)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/login (PUBLIC)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
}
