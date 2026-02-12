package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.domain.OrderStatus;
import com.techgadget.ecommerce.dto.request.CreateProductReviewRequest;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.ProductResponse;
import com.techgadget.ecommerce.dto.response.ProductReviewResponse;
import com.techgadget.ecommerce.dto.response.UserResponse;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductReviewResponse createReview(
            Long userId,
            Long productId,
            CreateProductReviewRequest request
    ) {
        // Get order id
        Long orderId = request.getOrderId();

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

        // 1. Check if the user order exists
        Order order = orderRepository
                .findUserOrderByIdWithRelation(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order not found with order id = {} and user id = {}",
                            orderId, userId);
                    return new NotFoundException("User hasn't ordered yet");
                });

        // 2. Check if the order status is completed
        if (!order.getOrderStatus().equals(OrderStatus.COMPLETED)) {
            log.warn("User {} is trying to review order {} with status {}",
                    userId, orderId, order.getOrderStatus());
            throw new ConflictException("You can only review products from completed orders");
        }

        // 3. Check if this order has that product
        boolean hasProduct = order.getItems().stream()
                .anyMatch(oi -> oi.getProduct().equals(product));

        if (!hasProduct) {
            log.warn("Product {} not found in order {} for user {}",
                    productId, orderId, userId);
            throw new NotFoundException("This product is not part of selected order");
        }

        // 4. Check if user already reviewed the product
        if (productReviewRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            log.warn("User {} already reviewed product {}", userId, productId);
            throw new ConflictException("You have already reviewed this product");
        }

        // 5. Create product review
        ProductReview productReview = new ProductReview();
        productReview.setUser(user);
        productReview.setProduct(product);
        productReview.setRating(request.getRating());
        productReview.setComment(request.getComment());

        productReviewRepository.save(productReview);

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

        Sort.Direction direction = getDirection(sortDir);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        Page<ProductReview> productReviewPage = productReviewRepository
                .findByProduct_IdWithRelation(productId, pageable);

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

        UserResponse userRes = new UserResponse();
        userRes.setId(pr.getUser().getId());
        userRes.setUsername(pr.getUser().getUsername());
        userRes.setEmail(pr.getUser().getEmail());
        userRes.setFullName(pr.getUser().getFullName());

        ProductReviewResponse res = new ProductReviewResponse();
        res.setId(pr.getId());
        res.setUser(userRes);
        res.setProductName(pr.getProduct().getName());
        res.setRating(pr.getRating());
        res.setComment(pr.getComment());
        res.setCreatedAt(pr.getCreatedAt());

        return res;
    }
}
