package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.product.CreateProductRequest;
import com.techgadget.ecommerce.dto.request.product.SearchProductRequest;
import com.techgadget.ecommerce.dto.response.ErrorResponse;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.product.ProductDetailResponse;
import com.techgadget.ecommerce.dto.response.product.ProductListResponse;
import com.techgadget.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    /**
     * Create new product (ADMIN)
     * POST /api/products
     */
    @Operation(
            summary = "Create new product",
            description = "Only admin can create new product."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order is created"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @PostMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ProductDetailResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        var productResponse = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(productResponse);
    }

    /**
     * Advanced search (name + @Nullable category + price) (PUBLIC)
     * Flexible sort
     * GET /api/products/search?....
     */
    @Operation(
            summary = "Search product",
            description = "Search product using query params filter",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody()
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Searched product is founded"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ProductListResponse>> searchProducts(
            @Valid @ModelAttribute SearchProductRequest request
    ) {
        var paginatedResponse = productService
                .searchProducts(request);

        return ResponseEntity.ok(paginatedResponse);
    }

    /**
     * Get single product by id (PUBLIC)
     * GET /api/products/{id}
     */
    @Operation(
            summary = "Get product by id",
            description = ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product by id is founded"),
            @ApiResponse(
                    responseCode = "4**",
                    description = "Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
    })
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductById(@PathVariable Long productId) {
        var productResponse = productService.getProductById(productId);

        return ResponseEntity.ok(productResponse);
    }

}
