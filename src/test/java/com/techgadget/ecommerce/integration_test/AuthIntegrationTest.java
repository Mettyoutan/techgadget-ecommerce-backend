package com.techgadget.ecommerce.integration_test;

import com.techgadget.ecommerce.dto.request.auth.LoginRequest;
import com.techgadget.ecommerce.dto.request.auth.RegisterRequest;
import com.techgadget.ecommerce.entity.RefreshToken;
import com.techgadget.ecommerce.repository.RefreshTokenRepository;
import com.techgadget.ecommerce.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for Auth endpoints
 *
 * Things this test verifies:
 * - Flyway migrations ran correctly
 * - PasswordEncoder works
 * - JWT token is generated and signed correctly
 * - HttpOnly cookie is set correctly
 * - ErrorResponse JSON format matches
 * - Database constraints (unique email/username) are enforced
 */
@DisplayName("Auth Integration Test")
public class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Clean up before each test (ensure isolation).
     */
    @BeforeEach
    void cleanUpDb() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("success - returns 201, user and refresh token persisted in DB")
        void success_returns201() throws Exception {

            RegisterRequest request = new RegisterRequest(
                    "username", "email@gmail.com", "password", "full name"
            );

            // Performing mock HTTP request through all filters
            // (DispatcherServlet -> RateLimitFilter -> AuthFilter -> Controller -> Service -> DB)
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                    // Status must 201, contentType must application/json
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                    // Verify JSON response with jsonPath
                    .andExpect(jsonPath("$.access").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value("email@gmail.com"))
                    .andExpect(jsonPath("$.user.username").value("username"))

                    // Verify refresh cookie exists and http only
                    .andExpect(cookie().exists("refresh"))
                    .andExpect(cookie().httpOnly("refresh", true));

            // Verify user is persisted in DB
            assertThat(userRepository.findByEmail("email@gmail.com")).isPresent();

            // Verify refresh token is persisted in DB
            assertThat(refreshTokenRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("duplicate email - returns 409")
        void duplicateEmail_returns409() throws Exception {

            // Arrange: User A registers with email "email@gmail.com"
            RegisterRequest requestA = new RegisterRequest(
                    "user a", "email@gmail.com", "password", "full name"
            );

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestA))
            )
                    // User A is created, must 201
                    .andExpect(status().isCreated());

            // Act: User B registers with email "email@gmail.com" (SAME EMAIL)
            RegisterRequest requestB = new RegisterRequest(
                    "user b", "email@gmail.com", "password", "full name"
            );

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestB))
            )
                    // Duplicate email, must 409
                    .andExpect(status().isConflict())

                    // Verify ErrorResponse json
                    .andExpect(jsonPath("$.message").value("Email or Username already registered."))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());

            // Verify DB only has 1 user and 1 refresh token
            assertThat(refreshTokenRepository.findAll()).hasSize(1);
            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("invalid request body - returns 400")
        void invalidRequestBody_returns400() throws Exception {

            RegisterRequest invalidRequest = new RegisterRequest(
                    "", "not-an-email", "12", ""
            );

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
                    // Must be 400
                    .andExpect(status().isBadRequest())

                    // Verify ErrorResponse json
                    .andExpect(jsonPath("$.message").value("Validation Failed."))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());

            // Verify DB doesn't persist anything
            assertThat(refreshTokenRepository.findAll()).hasSize(0);
            assertThat(userRepository.findAll()).hasSize(0);
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        /**
         * Arrange registered user for login testing
         */
        @BeforeEach
        void registerUser() throws Exception {

            RegisterRequest request = new RegisterRequest(
                    "username", "email@gmail.com", "password", "full name"
            );

            // Register user via endpoint
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            );
        }

        @Test
        @DisplayName("success - returns 200, generates new access & refresh token")
        void success_returns200() throws Exception {

            LoginRequest request = new LoginRequest(
                    "email@gmail.com", "password"
            );

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                    // Must be 200
                    .andExpect(status().isOk())

                    // Verify AuthResponse json
                    .andExpect(jsonPath("$.access").isNotEmpty())
                    .andExpect(jsonPath("$.message").value("Login Successful."))
                    .andExpect(jsonPath("$.user.email").value("email@gmail.com"))

                    // Verify refresh cookie exists
                    .andExpect(cookie().exists("refresh"))
                    .andExpect(cookie().httpOnly("refresh", true));

            // Verify there are 2 refresh token, 1 revoked and 1 active
            List<RefreshToken> refreshTokenList = refreshTokenRepository.findAll();
            assertThat(refreshTokenList).hasSize(2);
            assertThat(refreshTokenList.stream().filter(RefreshToken::isRevoked)).hasSize(1);
        }

        @Test
        @DisplayName("email not found - returns 401")
        void emailNotFound_returns401() throws Exception {

            LoginRequest request = new LoginRequest(
                    "notfound@gmail.com", "password"
            );

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                    // Must be 401
                    .andExpect(status().isUnauthorized())

                    // Verify ErrorResponse json
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid username or password."))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("password not match - returns 401")
        void passwordNotMatch_returns401() throws Exception {

            LoginRequest request = new LoginRequest(
                    "email@gmail.com", "RANDOM_PASSWORD"
            );

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                    // Must be 401
                    .andExpect(status().isUnauthorized())

                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid username or password."))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        private Cookie refreshCookie;

        @BeforeEach
        void setUp() throws Exception {

            RegisterRequest request = new RegisterRequest(
                    "username", "email@gmail.com", "password", "full name"
            );

            refreshCookie = mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                    .andReturn().getResponse().getCookie("refresh");

            assertThat(refreshCookie).isNotNull();
            assertThat(refreshCookie.getValue()).isNotEmpty();
            assertThat(refreshCookie.isHttpOnly()).isTrue();
        }

        @Test
        @DisplayName("success - returns 200, old refresh token revoked")
        void success_returns200() throws Exception {

            mockMvc.perform(post("/auth/refresh")
                    .cookie(refreshCookie)
            )

                    // Must be 200
                    .andExpect(status().isOk())

                    // Verify AuthResponse json
                    .andExpect(jsonPath("$.message").value("Refresh Successful."))
                    .andExpect(jsonPath("$.access").isNotEmpty())
                    .andExpect(jsonPath("$.user").exists())

                    // Verify refresh http cookie exists
                    .andExpect(cookie().exists("refresh"))
                    .andExpect(cookie().httpOnly("refresh", true));

            String oldCookieRefresh = refreshCookie.getValue();

            List<RefreshToken> refreshTokenList = refreshTokenRepository.findAll();
            assertThat(refreshTokenList).hasSize(2);
            assertThat(refreshTokenList
                    .stream()
                    .filter(r -> r.isRevoked() && r.getRefresh().equals(oldCookieRefresh))
                    .toList()
            ).hasSize(1);

        }
    }
}
