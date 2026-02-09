package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.CreateAddressRequest;
import com.techgadget.ecommerce.dto.response.UserProfileResponse;
import com.techgadget.ecommerce.security.CustomUserDetails;
import com.techgadget.ecommerce.service.UserProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        UserProfileResponse response = userProfileService
                .getUserProfile(userDetails.getUserId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/addresses")
    public ResponseEntity<UserProfileResponse> addAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateAddressRequest request
    ) {

        UserProfileResponse response = userProfileService
                .addAddress(userDetails.getUserId(), request);

        return ResponseEntity.ok(response);
    }
}
