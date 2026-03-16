package com.techgadget.ecommerce.unit_test;

import com.techgadget.ecommerce.dto.request.product.CreateProductRequest;
import com.techgadget.ecommerce.dto.request.product.SearchProductRequest;
import com.techgadget.ecommerce.dto.response.PaginatedResponse;
import com.techgadget.ecommerce.dto.response.product.ProductDetailResponse;
import com.techgadget.ecommerce.dto.response.product.ProductListResponse;
import com.techgadget.ecommerce.entity.Category;
import com.techgadget.ecommerce.entity.Product;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.CategoryRepository;
import com.techgadget.ecommerce.repository.ProductRepository;
import com.techgadget.ecommerce.service.ProductService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = new Category();
        ReflectionTestUtils.setField(category, "id", 1L);
        category.setName("Electronics");
        category.setDescription("");
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        product = new Product();
        ReflectionTestUtils.setField(product, "id", 1L);
        product.setCategory(category);
        product.setName("Phone");
        product.setDescription("");
        product.setStock(2);
        product.setPrice(100_000L);
        product.setSpecs(Map.of());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("getProductById()")
    class GetProductById {

        @Test
        @DisplayName("success - returns product detail response")
        void success() {

            when(productRepository.findProductDetailById(1L))
                    .thenReturn(Optional.of(product));

            ProductDetailResponse response = productService.getProductById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Phone");
            assertThat(response.getCategory()).isNotNull();
            assertThat(response.getDescription()).isEqualTo("");
            assertThat(response.getPrice()).isEqualTo(100000L);
            assertThat(response.getStock()).isEqualTo(2);
            assertThat(response.getSpecs()).isEqualTo(Map.of());

            verify(productRepository, times(1))
                    .findProductDetailById(1L);
        }

        @Test
        @DisplayName("product not found - throws NotFoundException")
        void productNotFound_throwsNotFoundException() {

            when(productRepository.findProductDetailById(1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Product not found.");

            verify(productRepository, times(1))
                    .findProductDetailById(1L);
        }
    }

    @Nested
    @DisplayName("searchProducts()")
    class SearchProducts {

        private Page<Product> singleProductPage;

        @BeforeEach
        void setUpSearch() {
            singleProductPage = new PageImpl<>(List.of(product));
        }

        @Test
        @DisplayName("invalid sort direction - default to DESC without throwing")
        void invalidSortDirection_defaultsToDesc() {

            SearchProductRequest request = new SearchProductRequest();
            request.setName("");
            request.setCategoryId(null);
            request.setMinPrice(null);
            request.setMaxPrice(null);
            request.setPage(0);
            request.setSize(10);
            request.setSortBy("createdAt");
            request.setSortDir("INVALID_DIRECTION"); // Invalid sort dir

            when(productRepository.findProductListByName(eq(""), any(Pageable.class)))
                    .thenReturn(singleProductPage);

            // Make sure Sort Direction parsing process don't throw exception
            assertThatNoException().isThrownBy(() -> productService.searchProducts(request));
        }

        @Nested
        @DisplayName("with category")
        class WithCategory {

            @Test
            @DisplayName("category not found - throws NotFoundException")
            void categoryNotFound_throwsNotFoundException() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(99L);
                request.setMinPrice(null);
                request.setMaxPrice(null);
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                // Category with id 99L is not found
                when(productRepository.existsByCategory_Id(99L))
                        .thenReturn(false);

                assertThatThrownBy(() -> productService.searchProducts(request))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("Category not found.");

                verify(productRepository, times(1))
                        .existsByCategory_Id(99L);
            }

            @Test
            @DisplayName("without price min and max price - calls correct method")
            void withoutMinAndMaxPrice_callsCorrectMethod() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(1L);
                request.setMinPrice(null);
                request.setMaxPrice(null);
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                when(productRepository.existsByCategory_Id(1L))
                        .thenReturn(true);

                when(productRepository.findProductListByNameAndCategory_Id(
                        eq(""), eq(1L), any(Pageable.class))
                ).thenReturn(singleProductPage);

                PaginatedResponse<ProductListResponse> response = productService.searchProducts(request);

                assertThat(response).isNotNull();
                assertThat(response.getTotalElements()).isEqualTo(1L);

                verify(productRepository, atLeastOnce())
                        .existsByCategory_Id(1L);
                verify(productRepository, atLeastOnce())
                        .findProductListByNameAndCategory_Id(eq(""), eq(1L), any(Pageable.class));
            }

            @Test
            @DisplayName("with min price, without max price - calls price greater than equal method")
            void withMinPrice_callsPriceGreaterThanEqualMethod() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(1L);
                request.setMinPrice(2000L); // Min price is 2000
                request.setMaxPrice(null);
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                when(productRepository.existsByCategory_Id(1L))
                        .thenReturn(true);

                when(productRepository.findProductListByNameAndCategory_IdAndPriceGreaterThanEqual(
                        eq(""), eq(1L), eq(2000L), any(Pageable.class))
                ).thenReturn(singleProductPage);

                PaginatedResponse<ProductListResponse> response = productService.searchProducts(request);

                assertThat(response).isNotNull();
                assertThat(response.getTotalElements()).isEqualTo(1L);

                verify(productRepository, atLeastOnce())
                        .existsByCategory_Id(1L);
                verify(productRepository, atLeastOnce())
                        .findProductListByNameAndCategory_IdAndPriceGreaterThanEqual(
                                eq(""), eq(1L), eq(2000L), any(Pageable.class));
            }

            @Test
            @DisplayName("with max price, without min price - calls price less than equal method")
            void withMaxPrice_callsPriceLessThanEqualMethod() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(1L);
                request.setMinPrice(null);
                request.setMaxPrice(1_000_000L); // Max price is 1 million
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                when(productRepository.existsByCategory_Id(1L))
                        .thenReturn(true);

                when(productRepository.findProductListByNameAndCategory_IdAndPriceLessThanEqual(
                        eq(""), eq(1L), eq(1_000_000L), any(Pageable.class))
                ).thenReturn(singleProductPage);

                PaginatedResponse<ProductListResponse> response = productService.searchProducts(request);

                assertThat(response).isNotNull();
                assertThat(response.getTotalElements()).isEqualTo(1L);

                verify(productRepository, atLeastOnce())
                        .existsByCategory_Id(1L);
                verify(productRepository, atLeastOnce())
                        .findProductListByNameAndCategory_IdAndPriceLessThanEqual(
                                eq(""), eq(1L), eq(1_000_000L), any(Pageable.class));
            }

            @Test
            @DisplayName("with min and max price - calls price between method")
            void withMinAndMaxPrice_callsPriceBetweenMethod() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(1L);
                request.setMinPrice(0L); // Min price is 0
                request.setMaxPrice(200_000L); // Max price is 200_000
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                when(productRepository.existsByCategory_Id(1L))
                        .thenReturn(true);

                when(productRepository.findProductListByNameAndCategory_IdAndPriceBetween(
                        eq(""), eq(1L), eq(0L),eq(200_000L), any(Pageable.class))
                ).thenReturn(singleProductPage);

                PaginatedResponse<ProductListResponse> response = productService.searchProducts(request);

                assertThat(response).isNotNull();
                assertThat(response.getTotalElements()).isEqualTo(1L);

                verify(productRepository, atLeastOnce())
                        .existsByCategory_Id(1L);
                verify(productRepository, atLeastOnce())
                        .findProductListByNameAndCategory_IdAndPriceBetween(
                                eq(""), eq(1L), eq(0L), eq(200_000L), any(Pageable.class));
            }
        }

        @Nested
        @DisplayName("without category")
        class WithoutCategory {

            @Test
            @DisplayName("without price min and max price - calls correct method")
            void withoutMinAndMaxPrice_callsCorrectMethod() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(null);
                request.setMinPrice(null);
                request.setMaxPrice(null);
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                when(productRepository.findProductListByName(
                        eq(""), any(Pageable.class))
                ).thenReturn(singleProductPage);

                PaginatedResponse<ProductListResponse> response = productService.searchProducts(request);

                assertThat(response).isNotNull();
                assertThat(response.getTotalElements()).isEqualTo(1L);

                verify(productRepository, atLeastOnce())
                        .findProductListByName(eq(""), any(Pageable.class));
            }

            @Test
            @DisplayName("with min price, without max price - calls price greater than equal method")
            void withMinPrice_callsPriceGreaterThanEqualMethod() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(null);
                request.setMinPrice(2000L); // Min price is 2000
                request.setMaxPrice(null);
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                when(productRepository.findProductListByNameAndPriceGreaterThanEqual(
                        eq(""), eq(2000L), any(Pageable.class))
                ).thenReturn(singleProductPage);

                PaginatedResponse<ProductListResponse> response = productService.searchProducts(request);

                assertThat(response).isNotNull();
                assertThat(response.getTotalElements()).isEqualTo(1L);

                verify(productRepository, atLeastOnce())
                        .findProductListByNameAndPriceGreaterThanEqual(
                                eq(""), eq(2000L), any(Pageable.class));
            }

            @Test
            @DisplayName("with max price, without min price - calls price less than equal method")
            void withMaxPrice_callsPriceLessThanEqualMethod() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(null);
                request.setMinPrice(null);
                request.setMaxPrice(1_000_000L); // Max price is 1 million
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                when(productRepository.findProductListByNameAndPriceLessThanEqual(
                        eq(""), eq(1_000_000L), any(Pageable.class))
                ).thenReturn(singleProductPage);

                PaginatedResponse<ProductListResponse> response = productService.searchProducts(request);

                assertThat(response).isNotNull();
                assertThat(response.getTotalElements()).isEqualTo(1L);

                verify(productRepository, atLeastOnce())
                        .findProductListByNameAndPriceLessThanEqual(
                                eq(""), eq(1_000_000L), any(Pageable.class));
            }

            @Test
            @DisplayName("with min and max price - calls price between method")
            void withMinAndMaxPrice_callsPriceBetweenMethod() {

                SearchProductRequest request = new SearchProductRequest();
                request.setName("");
                request.setCategoryId(null);
                request.setMinPrice(0L); // Min price is 0
                request.setMaxPrice(200_000L); // Max price is 200_000
                request.setPage(0);
                request.setSize(10);
                request.setSortBy("createdAt");
                request.setSortDir("desc");

                when(productRepository.findProductListByNameAndPriceBetween(
                        eq(""), eq(0L),eq(200_000L), any(Pageable.class))
                ).thenReturn(singleProductPage);

                PaginatedResponse<ProductListResponse> response = productService.searchProducts(request);

                assertThat(response).isNotNull();
                assertThat(response.getTotalElements()).isEqualTo(1L);

                verify(productRepository, atLeastOnce())
                        .findProductListByNameAndPriceBetween(
                                eq(""), eq(0L), eq(200_000L), any(Pageable.class));
            }
        }

    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("success - returns created product detail")
        void success() {

            CreateProductRequest request = new CreateProductRequest();
            request.setCategoryId(1L);
            request.setName("Samsung Galaxy S24");
            request.setDescription("Latest Samsung");
            request.setPrice(12_000_000L);
            request.setStock(5);
            request.setSpecs(Map.of("ram", "8GB"));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(category));

            ProductDetailResponse response = productService.createProduct(request);

            assertThat(response).isNotNull();
            assertThat(response.getName()).isNotNull();

            verify(categoryRepository, atLeastOnce())
                    .findById(1L);
            verify(productRepository, atLeastOnce())
                    .save(any(Product.class));
        }

        @Test
        @DisplayName("category not found - throws NotFoundException")
        void categoryNotFound_throwsNotFoundException() {

            CreateProductRequest request = new CreateProductRequest();
            request.setCategoryId(99L);
            request.setName("Samsung Galaxy S24");
            request.setDescription("Latest Samsung");
            request.setPrice(12_000_000L);
            request.setStock(5);
            request.setSpecs(Map.of("ram", "8GB"));

            when(categoryRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Category not found.");
        }
    }

}
