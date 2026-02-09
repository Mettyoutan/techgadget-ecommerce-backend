package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.request.CreateProductRequest;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.ProductResponse;
import com.techgadget.ecommerce.entity.Category;
import com.techgadget.ecommerce.entity.Product;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * Get all paginated products
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductResponse> getAllProducts(
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Getting all products");

        Sort.Direction sortDirection = getDirection(sortDir);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        Page<Product> productPage = productRepository.findAll(pageable);

        return mapPageToResponse(productPage);
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> {
            log.warn("Product not found with id {}", productId);
            return new NotFoundException("Product not found.");
        });

        return mapProductToResponse(product);
    }

    /**
     * Search products by name
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductResponse> searchByName(
            String name,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        Sort.Direction sortDirection = getDirection(sortDir);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCase(name, pageable);

        return mapPageToResponse(productPage);
    }

    /**
     * Filter products by category ID
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductResponse> filterByCategory(
            Long categoryId,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort.Direction sortDirection = getDirection(sortDir);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        Page<Product> productPage = productRepository.findByCategory_Id(categoryId, pageable);

        return mapPageToResponse(productPage);
    }

    /**
     * Filter products using range of price in rupiah
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductResponse> filterByPrice(
            Long minPrice,
            Long maxPrice,
            int page,
            int size,
            String sortDir
    ) {
        Sort.Direction sortDirection = getDirection(sortDir);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, "priceInRupiah")
        );

        Page<Product> productPage = productRepository.findByPriceInRupiahBetween(minPrice, maxPrice, pageable);

        return mapPageToResponse(productPage);
    }

    /**
     * Advanced search (name + category + price in rupiah)
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductResponse> advancedSearch(
            String name,
            Long categoryId,
            Long minPrice,
            Long maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
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

            productPage = productRepository.searchByNameAndCategory_IdAndPrice(
                    name, categoryId, minPrice, maxPrice, pageable
            );
        } else {
            productPage = productRepository.searchByNameAndPrice(name, minPrice, maxPrice, pageable);
        }

        return mapPageToResponse(productPage);
    }



    /**
     * Create product (admin)
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        // Check if category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found."));

        // Create new product
        Product product = new Product(
                category,
                request.getName(),
                request.getDescription(),
                request.getPriceInRupiah(),
                request.getStockQuantity(),
                request.getImageUrl(),
                request.getSpecs()
        );

        productRepository.save(product);

        log.info("Created new product: {} - Rp {}", product.getId(), product.getPriceInRupiah());

        return mapProductToResponse(product);
    }

    /**
     * Deduct stock (called during checkout)
     */
    @Transactional
    public void deductStock(Long productId, int stockQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found."));

        // Decrease the stock quantity
        product.decreaseStockQuantity(stockQuantity);

        // Saved
        productRepository.save(product);

        log.info("Deducted stock for product: {} - quantity: {}", productId, stockQuantity);
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
    private PaginatedResponse<ProductResponse> mapPageToResponse(Page<Product> productPage) {
        PaginatedResponse<ProductResponse> response = new PaginatedResponse<>();
        // Map Page<Product> into List<ProductResponse>
        response.setContent(productPage.map(this::mapProductToResponse).toList());
        response.setPageNumber(productPage.getNumber());
        response.setPageSize(productPage.getSize());
        response.setTotalPages(productPage.getTotalPages());
        response.setTotalElements(productPage.getTotalElements());
        response.setHasNextPage(productPage.hasNext());
        response.setHasPreviousPage(productPage.hasPrevious());

        return response;
    }

    /**
     * Helper method for build product response
     */
    private ProductResponse mapProductToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPriceInRupiah(product.getPriceInRupiah());
        response.setStockQuantity(product.getStockQuantity());
        response.setImageUrl(product.getImageUrl());
        response.setSpecs(product.getSpecs());

        ProductResponse.CategoryDto categoryDto = new ProductResponse.CategoryDto(
                product.getCategory().getId(),
                product.getCategory().getName()
        );
        response.setCategory(categoryDto);

        return response;
    }
}
