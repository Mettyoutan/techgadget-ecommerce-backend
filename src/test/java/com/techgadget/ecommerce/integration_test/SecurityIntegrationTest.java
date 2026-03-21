package com.techgadget.ecommerce.integration_test;

import com.techgadget.ecommerce.dto.request.auth.LoginRequest;
import com.techgadget.ecommerce.dto.request.auth.RegisterRequest;
import com.techgadget.ecommerce.dto.request.product.CreateProductRequest;
import com.techgadget.ecommerce.dto.response.auth.AuthResponse;
import com.techgadget.ecommerce.entity.Category;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.enums.UserRole;
import com.techgadget.ecommerce.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Security Integration Test")
public class SecurityIntegrationTest extends BaseIntegrationTest{

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;

    /**
     * Helper method to get customer access token
     */
    private String getAccessTokenForCustomer() throws Exception {

        userRepository.deleteAll();

        // Register new user with role customer
        RegisterRequest registerRequest = new RegisterRequest(
                "username", "email@gmail.com", "password", "full name"
        );

        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
                .andExpect(status().isCreated())
                .andReturn();

        String jsonResponseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(jsonResponseBody, AuthResponse.class);

        return authResponse.getAccess();
    }

    /**
     * Helper method to get admin access token
     */
    private String getAccessTokenForAdmin() throws Exception {

        userRepository.deleteAll();

        // Register new user with role customer
        RegisterRequest registerRequest = new RegisterRequest(
                "username", "email@gmail.com", "password", "full name"
        );
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isCreated());

        // Get registered user
        User user = userRepository.findByEmail("email@gmail.com")
                .orElse(null);
        assertThat(user).isNotNull();

        // Change role from customer -> admin
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);

        // Login to get new access token
        LoginRequest loginRequest = new LoginRequest(
                "email@gmail.com", "password"
        );
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(jsonResponseBody, AuthResponse.class);

        return authResponse.getAccess();
    }


    @Nested
    @DisplayName("Public endpoints - accessible without token")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /products - without token, returns 200")
        void getProducts_noToken_returns200() throws Exception {

            mockMvc.perform(get("/products/search"))
                    // Must be 200
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /auth/login - without token, returns 200")
        void login_noToken_returns200() throws Exception {

            RegisterRequest registerRequest = new RegisterRequest(
                    "username", "email@gmail.com", "password", "full name"
            );

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                    .andExpect(status().isCreated());

            LoginRequest request = new LoginRequest(
                    "email@gmail.com", "password"
            );

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                    // Must be 200
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Protected endpoints - require valid JWT")
    class ProtectedEndpoints {

        @Test
        @DisplayName("GET /cart - without token, returns 401")
        void getCart_noToken_returns401() throws Exception {

            mockMvc.perform(get("/cart"))
                    // Must be 401
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /cart - with valid token, returns 200")
        void getCart_withValidToken_returns200() throws Exception {

            String access = getAccessTokenForCustomer();

            mockMvc.perform(get("/cart")
                    .header("Authorization", "Bearer " + access)
            )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("get cart - with invalid token, returns 401")
        void getCart_withInvalidToken_returns401() throws Exception {

            mockMvc.perform(get("/cart")
                    .header("Authorization", "Bearer invalid.jwt")
            )
                    // Must be 401
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Admin endpoints - require admin role")
    class AdminEndpoints {

        /**
         * Helper method to build CreateProductRequest
         */
        private CreateProductRequest buildCreateProductRequest() throws Exception {

            categoryRepository.deleteAll();

            // Category service not created yet, so build category manually
            Category category = new Category(
                    "Phone", ""
            );
            category = categoryRepository.save(category);

            return new CreateProductRequest(
                    category.getId(),
                    "Iphone 17",
                    "",
                    20_000_000L,
                    20,
                    Map.of()
            );
        }

        @Test
        @DisplayName("Create product - as customer, returns 403")
        void createProduct_asCustomer_returns403() throws Exception {

            String customerToken = getAccessTokenForCustomer();

            mockMvc.perform(post("/products")
                    .header("Authorization", "Bearer " + customerToken)

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildCreateProductRequest()))
            )
                    // Must be 403
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("create product - as admin, returns 201")
        void createProduct_asAdmin_returns201() throws Exception {

            String adminToken = getAccessTokenForAdmin();

            mockMvc.perform(post("/products")
                    .header("Authorization", "Bearer " + adminToken)

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildCreateProductRequest()))
            )
                    // Must be 201
                    .andExpect(status().isCreated());
        }
    }
}
