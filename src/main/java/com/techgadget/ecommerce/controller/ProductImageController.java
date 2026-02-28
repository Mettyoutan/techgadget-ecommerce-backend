package com.techgadget.ecommerce.controller;

import com.techgadget.ecommerce.dto.response.image.ImageResponse;
import com.techgadget.ecommerce.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/products/{productId}/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    /**
     * Upload product image
     * -
     * Consumes multipart/form-data
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ImageResponse> upload(
            @PathVariable Long productId,
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary
    ) {
        ImageResponse response = productImageService.upload(productId, file, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<?> delete(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        productImageService.delete(productId, imageId);
        return ResponseEntity.noContent().build();
    }
}
