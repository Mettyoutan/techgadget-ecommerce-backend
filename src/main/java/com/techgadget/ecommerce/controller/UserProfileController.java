package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.CreateAddressRequest;
import com.techgadget.ecommerce.dto.response.ErrorResponse;
import com.techgadget.ecommerce.dto.response.UserProfileResponse;
import com.techgadget.ecommerce.security.CustomUserDetails;
import com.techgadget.ecommerce.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(
            summary = "Get user profile",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile is found"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        UserProfileResponse response = userProfileService
                .getUserProfile(userDetails.getUserId());

        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Add address to profile",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address is added"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PostMapping("/addresses")
    public ResponseEntity<UserProfileResponse> addAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateAddressRequest request
    ) {

        UserProfileResponse response = userProfileService
                .addAddress(userDetails.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
