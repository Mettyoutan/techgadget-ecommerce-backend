package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.enums.OrderStatus;
import com.techgadget.ecommerce.dto.request.product.CreateProductReviewRequest;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.product.ProductReviewResponse;
import com.techgadget.ecommerce.dto.response.user.UserResponse;
import com.techgadget.ecommerce.entity.*;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.OrderRepository;
import com.techgadget.ecommerce.repository.ProductRepository;
import com.techgadget.ecommerce.repository.ProductReviewRepository;
import com.techgadget.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // TODO: create user order validation before create review
    @Transactional
    public ProductReviewResponse createReview(
            Long userId,
            Long productId,
            CreateProductReviewRequest request
    ) {

        log.debug("Processing create review - User: {}, Product: {}, Rating: {}",
                userId, productId, request.getRating());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id {}", userId);
                    return new NotFoundException("User not found");
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found with id {}", productId);
                    return new NotFoundException("Product not found");
                });

        // Check if user has ordered the product
        OrderItem toBeReviewedItem = null;

        List<Order> userOrders = orderRepository
                .findUserOrderByUserId(userId);

        for (Order order : userOrders) {
            OrderItem item = order.getItems().stream()
                    .filter(i ->
                            i.getProductIdSnapshot().equals(productId)
                            && !i.isReviewed()
                    )
                    .findFirst()
                    .orElse(null);
            if (item != null) {
                toBeReviewedItem = item;
                break;
            }
        }

        if (toBeReviewedItem == null) {
            log.warn("User {} tried to review Product {} but hasn't been ordered or already been reviewed",
                    userId, productId);
            throw new ConflictException("Product has not been ordered or already been reviewed.");
        }

        // Create product review
        ProductReview productReview = new ProductReview(
                user,
                toBeReviewedItem,
                product,
                request.getRating(),
                request.getComment()
        );
        productReviewRepository.save(productReview);

        log.info("User {} successfully created review {} for product {}",
                userId, productReview.getId(), productId);

        return mapProductReviewToResponse(productReview);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ProductReviewResponse> getSingleProductReviews(
            Long productId,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        log.debug("Processing get single product reviews - Product: {}", productId);

        Sort.Direction direction = getDirection(sortDir);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        Page<ProductReview> productReviewPage = productReviewRepository
                .findByProduct_IdWithRelation(productId, pageable);

        log.info("Successfully fetched {} product reviews for product {}",
                productReviewPage.getTotalElements(), productId);

        return mapPageToResponse(productReviewPage);
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

    private PaginatedResponse<ProductReviewResponse> mapPageToResponse(Page<ProductReview> page) {
        PaginatedResponse<ProductReviewResponse> response = new PaginatedResponse<>();
        // Map Page<Product> into List<ProductResponse>
        response.setContent(page.map(this::mapProductReviewToResponse).toList());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPages(page.getTotalPages());
        response.setTotalElements(page.getTotalElements());
        response.setHasNextPage(page.hasNext());
        response.setHasPreviousPage(page.hasPrevious());

        return response;
    }

    private ProductReviewResponse mapProductReviewToResponse(ProductReview pr) {

        // Create user response
        User user = pr.getUser();
        UserResponse userRes = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
        );

        return new ProductReviewResponse(
                pr.getId(),
                pr.getProduct().getName(),
                pr.getRating(),
                pr.getComment(),
                pr.getCreatedAt(),
                userRes
        );
    }
}
