package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.response.image.ImageResponse;
import com.techgadget.ecommerce.dto.response.image.StoredImageDto;
import com.techgadget.ecommerce.entity.Product;
import com.techgadget.ecommerce.entity.ProductImage;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.ProductImageRepository;
import com.techgadget.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    private final MinioStorageService minioStorageService;

    /**
     * Upload product image
     */
    @Transactional
    public ImageResponse upload(Long productId, MultipartFile file, boolean isPrimary) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product {} not found.", productId);
                    return new NotFoundException("Product not found.");
                });

        // Get all product images
        List<ProductImage> images = product.getImages();

        // Store image into MinIO
        String objectKey = generateObjectKey(productId, file);
        StoredImageDto imageDto = minioStorageService.store(file, objectKey);

        // Create product image & save
        ProductImage createdImage = new ProductImage();
        createdImage.setProduct(product);
        createdImage.setObjectKey(imageDto.objectKey());
        createdImage.setThumbnailKey(imageDto.thumbnailKey()); // NULLABLE

        /*
            IsPrimary validation
            -
            Prevent:
            1) No primary image available
            2) Duplicate primary image
         */
        ProductImage primaryImage = images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .orElse(null);
        if (isPrimary && primaryImage != null) {
            // Set new primary image
            primaryImage.setPrimary(false);
            createdImage.setPrimary(true);
        } else if (!isPrimary && primaryImage == null) {
            // Force createdImage to be primary
            createdImage.setPrimary(true);
        } else {
            createdImage.setPrimary(isPrimary);
        }

        productImageRepository.save(createdImage);

        // If thumbnail exists, use thumbnail key
        if (createdImage.getThumbnailKey() != null) {
            return new ImageResponse(createdImage.getId(), createdImage.getThumbnailKey(), createdImage.isPrimary());
        }

        return new ImageResponse(createdImage.getId(), createdImage.getObjectKey(), createdImage.isPrimary());
    }

    /**
     * Delete a product image from MinIO and DB
     */
    @Transactional
    public void delete(Long productId, Long productImageId) {

        // Get product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product {} not found.", productId);
                    return new NotFoundException("Product not found.");
                });

        // Get product image to be deleted
        ProductImage image = productImageRepository
                .findByIdAndProduct_Id(productImageId, productId)
                .orElseThrow(() -> {
                    log.warn("Product image {} not found - Product:{}",
                            productImageId, productId);
                    return new NotFoundException("Product image not found.");
                });

        /*
            If primary, make any image the next primary
         */
        if (image.isPrimary()) {
            ProductImage newPrimary = product.getImages()
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (newPrimary != null) {
                newPrimary.setPrimary(true);
                productImageRepository.save(newPrimary);
            }
        }

        // Delete object image
        minioStorageService.delete(image.getObjectKey());

        // Delete thumbnail image
        if (image.getThumbnailKey() != null) {
            minioStorageService.delete(image.getThumbnailKey());
        }

        productImageRepository.delete(image);
    }

    public String getImageUrl(String objectOrThumbnailKey) {
        return minioStorageService.generateViewUrl(objectOrThumbnailKey);
    }

    /**
     * Generate custom object key
     */
    private String generateObjectKey(Long productId, MultipartFile file) {

        // ext (.jpg, .png, etc)
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".") + 1))
                .orElse("jpg"); // Default is jpg

        return "products/%d/%s.%s"
                .formatted(productId, UUID.randomUUID(), ext);
    }
}
