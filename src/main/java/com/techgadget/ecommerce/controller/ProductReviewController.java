package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.CreateProductReviewRequest;
import com.techgadget.ecommerce.dto.request.PaginationRequest;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.ProductReviewResponse;
import com.techgadget.ecommerce.security.CustomUserDetails;
import com.techgadget.ecommerce.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products/{productId}/reviews")
@RequiredArgsConstructor
@Validated
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    /**
     * CUSTOMER ONLY
     * -
     * Create review for single product
     */
    @PostMapping
    public ResponseEntity<ProductReviewResponse> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductReviewRequest request
    ) {

        ProductReviewResponse response = productReviewService
                .createReview(
                        userDetails.getUserId(),
                        productId,

                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUBLIC ENDPOINT
     * -
     * Get paginated review from single product
     */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<PaginatedResponse<ProductReviewResponse>> getSingleProductReviews(
            @PathVariable Long productId,
            @Valid @ModelAttribute PaginationRequest request
    ) {

        PaginatedResponse<ProductReviewResponse> response =
                productReviewService.getSingleProductReviews(
                        productId,
                        request.getPage(),
                        request.getSize(),
                        request.getSortBy(),
                        request.getSortDir()
                );

        return ResponseEntity.ok(response);
    }
}
