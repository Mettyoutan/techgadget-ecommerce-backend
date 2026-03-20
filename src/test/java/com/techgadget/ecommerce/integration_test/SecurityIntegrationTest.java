package com.techgadget.ecommerce.integration_test;

import com.techgadget.ecommerce.dto.request.auth.LoginRequest;
import com.techgadget.ecommerce.dto.request.auth.RegisterRequest;
import com.techgadget.ecommerce.dto.response.auth.AuthResponse;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.repository.RefreshTokenRepository;
import com.techgadget.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Security Integration Test")
public class SecurityIntegrationTest extends BaseIntegrationTest{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Helper method to get customer access token
     * Register -> getAccess
     */
    private String getAccessTokenForCustomer() throws Exception {

        // Register new user
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

            mockMvc.perform(get("/cart/"))
                    // Must be 401
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /cart - with valid token, returns 200")
        void getCart_withValidToken_return200() throws Exception {

            String access = getAccessTokenForCustomer();

            mockMvc.perform(get("/cart/")
                    .header("Authorization", "Bearer " + access)
            )
                    .andExpect(status().isOk());
        }
    }
}
