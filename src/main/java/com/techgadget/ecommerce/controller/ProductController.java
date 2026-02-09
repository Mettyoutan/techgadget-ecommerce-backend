package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.request.CreateProductRequest;
import com.techgadget.ecommerce.dto.request.ProductSearchRequest;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.ProductResponse;
import com.techgadget.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @PostMapping
    @PreAuthorize("isAuthenticated()") // TODO: using ADMIN role instead of just authenticated
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        var productResponse = productService.createProduct(request);

        return ResponseEntity.ok(productResponse);
    }

    /**
     * Advanced search (name + @Nullable category + price) (PUBLIC)
     * Flexible sort
     * GET /api/products/search?....
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ProductResponse>> searchProducts(
            @Valid @ModelAttribute ProductSearchRequest request
    ) {
        var paginatedResponse = productService.advancedSearch(
                request.getName(),
                request.getCategoryId(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getSortDir()
        );

        return ResponseEntity.ok(paginatedResponse);
    }

    /**
     * Get single product by id (PUBLIC)
     * GET /api/products/{id}
     */
    @GetMapping("{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("id") Long productId) {
        var productResponse = productService.getProductById(productId);

        return ResponseEntity.ok(productResponse);
    }

    /**
     * Get all products (PUBLIC)
     * Flexible sort
     * GET /api/products?..
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ProductResponse>> getAllProducts(
            @Valid @ModelAttribute ProductSearchRequest request
    ) {
        var paginatedResponse = productService.getAllProducts(
                        request.getPage(),
                        request.getSize(),
                        request.getSortBy(), // Can decide sort by what
                        request.getSortDir()
                );

        return ResponseEntity.ok(paginatedResponse);
    }

}
