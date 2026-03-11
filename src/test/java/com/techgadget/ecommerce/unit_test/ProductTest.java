package com.techgadget.ecommerce.unit_test;

import com.techgadget.ecommerce.dto.response.product.ProductDetailResponse;
import com.techgadget.ecommerce.dto.response.product.ProductListResponse;
import com.techgadget.ecommerce.entity.Category;
import com.techgadget.ecommerce.entity.Product;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.CategoryRepository;
import com.techgadget.ecommerce.repository.ProductRepository;
import com.techgadget.ecommerce.service.ProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ProductTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setDescription("");
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        product = new Product();
        product.setId(1L);
        product.setCategory(category);
        product.setName("Phone");
        product.setDescription("");
        product.setStockQuantity(2);
        product.setPriceInRupiah(100000L);
        product.setSpecs(Map.of());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void test_getProductById_success() {

        Mockito.when(productRepository.findById(Mockito.any()))
                .thenReturn(Optional.of(product));

        ProductDetailResponse response =
                productService.getProductById(1L);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getName());
        Assertions.assertNotNull(response.getCategory());
        Assertions.assertNotNull(response.getId());
        Assertions.assertNotNull(response.getPriceInRupiah());
        Assertions.assertNotNull(response.getStockQuantity());

        Mockito.verify(productRepository, Mockito.times(1))
                .findById(Mockito.any());
    }

    @Test
    void test_getProductById_throwNotFound() {

        Mockito.when(productRepository.findById(Mockito.any()))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class,
                () -> productService.getProductById(1L));
    }

    @Test
    void test_advancedSearch_withoutCategory_success() {

        // Set category to null
        product.setCategory(null);

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by("createdAt").descending()
        );
    }
}
