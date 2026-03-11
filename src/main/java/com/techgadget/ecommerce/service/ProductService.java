package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.request.product.CreateProductRequest;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.image.ImageResponse;
import com.techgadget.ecommerce.dto.response.product.CategoryResponse;
import com.techgadget.ecommerce.dto.response.product.ProductDetailResponse;
import com.techgadget.ecommerce.dto.response.product.ProductListResponse;
import com.techgadget.ecommerce.entity.Category;
import com.techgadget.ecommerce.entity.Product;
import com.techgadget.ecommerce.entity.ProductImage;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.CategoryRepository;
import com.techgadget.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageService productImageService;

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long productId) {

        log.debug("Processing get product by id - Product: {}", productId);

        Product product = productRepository
                .findProductDetailById(productId) // Get single detail product
                .orElseThrow(() -> {
                    log.warn("Product not found with id {}", productId);
                    return new NotFoundException("Product not found.");
                });


        log.info("Successfully fetched product {}", product.getId());

        return mapToProductDetailResponse(product);
    }

    /**
     * Advanced search (name + category + price in rupiah)
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductListResponse> advancedSearch(
            String name,
            Long categoryId,
            Long minPrice,
            Long maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        log.debug("Processing advanced search - " +
                "ProductName: {}, Category: {}, MinPrice: {}, " +
                "MaxPrice: {}, Page: {}, Size: {}, SortBy: {}, SortDir: {}",
                name, categoryId, minPrice, maxPrice, page, size, sortBy, sortDir);

        Sort.Direction direction = getDirection(sortDir);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        Page<Product> productPage;

        if (categoryId != null) {
            // make sure category exists
            if (!productRepository.existsByCategory_Id(categoryId)) {
                throw new NotFoundException("Category not found.");
            }

            if (minPrice == null || maxPrice == null) {
                productPage = productRepository.findProductListByNameAndCategory_Id(
                        name, categoryId, pageable);
            } else {
                productPage = productRepository.findProductListByNameAndCategory_IdAndPrice(
                        name, categoryId, minPrice, maxPrice, pageable);
            }

            log.debug("Running query -> productRepository.searchByNameAndCategory_IdAndPrice");

        } else {
            if (minPrice == null || maxPrice == null) {
                productPage = productRepository.findProductListByName(
                        name, pageable);
            } else {
                productPage = productRepository.findProductListByNameAndPrice(
                        name, minPrice, maxPrice, pageable);
            }

            log.debug("Running query -> productRepository.searchByNameAndPrice");
        }

        log.info("Successfully fetched {} products using advanced search - Page: {}/{}",
                productPage.getNumberOfElements(), page, productPage.getTotalPages());

        return mapToPaginatedProductListResponse(productPage);
    }


    /**
     * Create product (admin)
     */
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {

        log.debug("Processing create product (ADMIN) - Category: {}, ProductName: {}, Stock: {}",
                request.getCategoryId(), request.getName(), request.getStock());

        // Check if category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found."));

        // Create new product (WITHOUT image)
        Product product = new Product(
                category,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock(),
                request.getSpecs()
        );

        productRepository.save(product);

        log.info("Successfully created product {} - Price: {}, Stock: {}",
                product.getId(),
                product.getPrice(),
                product.getStock());

        return mapToProductDetailResponse(product);
    }

    /**
     * Deduct stock (called during checkout)
     */
    @Transactional
    public void deductStock(Long productId, int quantity) {

        log.debug("Processing deduct stock by {} quantity - Product: {}",
                productId, quantity);

        // Find product without any relation
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found."));

        // Decrease the stock quantity
        product.decreaseStock(quantity);

        // Saved
        productRepository.save(product);

        log.info("Successfully deducted stock by {} quantity for product {} - ",
                productId, quantity);
    }

    /**
     * Helper method for validate & get sort direction
     */
    private Sort.Direction getDirection(String sortDir) {
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sort direction '{}', defaulting to DESC", sortDir);
            direction = Sort.Direction.DESC;
        }
        return direction;
    }

    /**
     * Helper method for build paginated response
     */
    private PaginatedResponse<ProductListResponse> mapToPaginatedProductListResponse(
            Page<Product> productPage) {
        PaginatedResponse<ProductListResponse> response = new PaginatedResponse<>();
        // Map Page<Product> into List<ProductListResponse>
        response.setContent(productPage.map(this::mapToProductListResponse).toList());
        response.setPageNumber(productPage.getNumber());
        response.setPageSize(productPage.getSize());
        response.setTotalPages(productPage.getTotalPages());
        response.setTotalElements(productPage.getTotalElements());
        response.setHasNextPage(productPage.hasNext());
        response.setHasPreviousPage(productPage.hasPrevious());

        return response;
    }

    /**
     * Helper method for build ProductList response
     */
    private ProductListResponse mapToProductListResponse(Product product) {

        /*
            - Get primary image key
            - Find the image url
         */
        String imageUrl = null;
        String imageKey = product.getPrimaryImageKey();

        if (imageKey != null) {
            imageUrl = productImageService.getImageUrl(
                    product.getPrimaryImageKey());
        }

        // Build category response
        CategoryResponse categoryRes = new CategoryResponse(
                product.getCategory().getId(),
                product.getCategory().getName()
        );

        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                imageUrl,
                product.getSpecs(),
                categoryRes
        );
    }

    /**
     * Helper method for build ProductDetail response
     */
    private ProductDetailResponse mapToProductDetailResponse(Product product) {


        // Get all image urls for product detail
        List<ImageResponse> imageResponses = product.getImages()
                .stream()
                .map(i -> {

                    String imageKey = i.getThumbnailKey() != null
                            ? i.getThumbnailKey()
                            : i.getOriginalKey();

                    String url = productImageService.getImageUrl(imageKey);
                    if (url == null) return null;

                    return new ImageResponse(url, i.isPrimary());
                })
                .toList();

        CategoryResponse categoryRes = new CategoryResponse(
                product.getCategory().getId(),
                product.getCategory().getName()
        );

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                imageResponses,
                product.getSpecs(),
                categoryRes
        );
    }
}
