package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.LoginRequest;
import com.techgadget.ecommerce.dto.request.RegisterRequest;
import com.techgadget.ecommerce.dto.response.AuthResponse;
import com.techgadget.ecommerce.dto.response.AuthServiceResponse;
import com.techgadget.ecommerce.dto.response.ErrorResponse;
import com.techgadget.ecommerce.exception.BadRequestException;
import com.techgadget.ecommerce.security.CustomUserDetails;
import com.techgadget.ecommerce.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.Column;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiryInMs;

    private final AuthService authService;

    /**
     * POST /api/auth/register (PUBLIC)
     */
    @Operation(
            summary = "Register new user",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletResponse response
    ) {
        AuthServiceResponse authServiceResponse = authService.register(registerRequest);

        // Add refresh to http cookie
        addRefreshToCookie(authServiceResponse.getRefresh(), response);

        return ResponseEntity.ok(mapToAuthResponse(authServiceResponse));
    }

    /**
     * POST /api/auth/login (PUBLIC)
     */
    @Operation(
            summary = "Login the user",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User login"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        AuthServiceResponse authServiceResponse = authService.login(loginRequest);

        // Add refresh to http cookie
        addRefreshToCookie(authServiceResponse.getRefresh(), response);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToAuthResponse(authServiceResponse));
    }

    /**
     * POST /api/auth/refresh (PUBLIC)
     */
    @Operation(
            summary = "Refresh the token",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User refresh"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = "refresh", defaultValue = "") String refresh,
            HttpServletResponse response
    ) {
        AuthServiceResponse authServiceResponse = authService.refresh(refresh);

        // Add refresh to httpCookie
        addRefreshToCookie(authServiceResponse.getRefresh(), response);

        return ResponseEntity.ok(mapToAuthResponse(authServiceResponse));
    }

    /**
     * POST /api/auth/logout (AUTHENTICATED)
     */
    @Operation(
            summary = "Logout the user",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User logout"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @CookieValue(value = "refresh", defaultValue = "") String refresh,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.logout(userDetails.getUserId(), refresh);

        // Delete refresh from cookie
        Cookie cookie = new Cookie("refresh", refresh);
        cookie.setPath("/api/auth/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);

        response.addCookie(cookie);

        return ResponseEntity.ok(authResponse);
    }

    private void addRefreshToCookie(String refresh, HttpServletResponse response) {
        int maxAge = (int) Duration.ofMillis(refreshExpiryInMs).toSeconds();

        Cookie cookie = new Cookie("refresh", refresh);
        cookie.setPath("/api/auth/");
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);

        response.addCookie(cookie);
    }

    private AuthResponse mapToAuthResponse(AuthServiceResponse authServiceResponse) {
        return new AuthResponse(
                authServiceResponse.getMessage(),
                authServiceResponse.getAccess(),
                authServiceResponse.getUser()
        );
    }
}
