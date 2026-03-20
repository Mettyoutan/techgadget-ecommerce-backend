package com.techgadget.ecommerce.unit_test;

import com.techgadget.ecommerce.dto.request.auth.LoginRequest;
import com.techgadget.ecommerce.dto.request.auth.RegisterRequest;
import com.techgadget.ecommerce.dto.response.auth.AuthResponse;
import com.techgadget.ecommerce.dto.response.auth.AuthServiceResponse;
import com.techgadget.ecommerce.entity.RefreshToken;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.exception.UnauthorizedException;
import com.techgadget.ecommerce.repository.RefreshTokenRepository;
import com.techgadget.ecommerce.repository.UserRepository;
import com.techgadget.ecommerce.security.JwtTokenProvider;
import com.techgadget.ecommerce.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    // Shared user test data
    private User savedUser;

    @BeforeEach
    void setUp() {
        // Set auth service field
        ReflectionTestUtils.setField(authService, "refreshExpirationInMs", 604800000);

        savedUser = new User(
                "username",
                "email@gmail.com",
                "hashed",
                "full name"
        );
        ReflectionTestUtils.setField(savedUser, "id", 1L);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterRequest request;

        @BeforeEach
        void setUp() {
            // Build register request
            request = new RegisterRequest(
                    "username",
                    "email@gmail.com",
                    "password",
                    "full name"
            );
        }

        @Test
        @DisplayName("success - returns auth service response")
        void success() {

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            when(passwordEncoder.encode("password"))
                    .thenReturn("hashed");
            when(userRepository.save(userCaptor.capture()))
                    .thenReturn(savedUser);
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString()))
                    .thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                    .thenReturn("refresh-token");

            AuthServiceResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Registration Successful.");
            assertThat(response.getAccess()).isEqualTo("access-token");
            assertThat(response.getRefresh()).isEqualTo("refresh-token");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("email@gmail.com");

            // Verify if password is hashed before saved
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getPassword())
                    .isEqualTo("hashed")
                    .isNotEqualTo("password");

            verify(refreshTokenRepository, times(1))
                    .save(any(RefreshToken.class));
            verify(userRepository, times(1))
                    .save(any(User.class));
        }

        @Test
        @DisplayName("duplicate email or username - throws ConflictException")
        void duplicateEmailOrUsername_throwsConflictException() {

            // Duplicate will throw DataIntegrityViolationException
            when(userRepository.save(any(User.class)))
                    .thenThrow(DataIntegrityViolationException.class);

            Assertions.assertThrows(ConflictException.class,
                    () -> authService.register(request));
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest request;

        @BeforeEach
        void setUp() {
            request = new LoginRequest(
                    "email@gmail.com",
                    "password"
            );
        }

        @Test
        @DisplayName("success - returns auth service response")
        void success() {

            when(userRepository.findByEmail("email@gmail.com"))
                    .thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches("password", "hashed"))
                    .thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(1L, "email@gmail.com"))
                    .thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(1L, "email@gmail.com"))
                    .thenReturn("refresh-token");

            AuthServiceResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Login Successful.");
            assertThat(response.getAccess()).isEqualTo("access-token");
            assertThat(response.getRefresh()).isEqualTo("refresh-token");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("email@gmail.com");

            // Verify if all active refresh token is revoked
            verify(refreshTokenRepository, times(1))
                    .revokeAllUnrevokedByUser_Id(1L);
            verify(refreshTokenRepository, times(1))
                    .save(any(RefreshToken.class));
            verify(userRepository, times(1))
                    .findByEmail("email@gmail.com");
        }

        @Test
        @DisplayName("email not found - throws UnauthorizedException")
        void emailNotFound_throwsUnathorizedException() {

            when(userRepository.findByEmail("email@gmail.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid username or password.");
        }

        @Test
        @DisplayName("password not match - throw UnauthorizedException")
        void passwordNotMatch_throwUnauthorizedException() {

            when(userRepository.findByEmail("email@gmail.com"))
                    .thenReturn(Optional.of(savedUser));

            // When password not match
            when(passwordEncoder.matches("password", "hashed"))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid username or password.");
        }
    }

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        private Claims claims;
        private String refresh;

        @BeforeEach
        void setUp() {
            refresh = "some-refresh-token";

            claims = Jwts.claims()
                    .subject("1")
                    .add("email", "email@gmail.com")
                    .build();
        }

        @Test
        @DisplayName("success - returns auth service response")
        void success() {

            RefreshToken activeRefreshToken = new RefreshToken(
                    savedUser, refresh, Instant.now().plus(10, ChronoUnit.HOURS)
            );

            when(jwtTokenProvider.validateToken("some-refresh-token"))
                    .thenReturn(claims);
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(savedUser));
            when(refreshTokenRepository.findByUser_IdAndRefreshAndRevokedIsFalse(1L, "some-refresh-token"))
                    .thenReturn(Optional.of(activeRefreshToken));
            when(jwtTokenProvider.generateAccessToken(1L, "email@gmail.com"))
                    .thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(1L, "email@gmail.com"))
                    .thenReturn("refresh-token");

            AuthServiceResponse response = authService.refresh(refresh);

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Refresh Successful.");
            assertThat(response.getAccess()).isEqualTo("access-token");
            assertThat(response.getRefresh()).isEqualTo("refresh-token");
            assertThat(response.getUser()).isNotNull();

            verify(refreshTokenRepository, times(1))
                    .save(any(RefreshToken.class));
            // Verify if all active token revoked
            verify(refreshTokenRepository, times(1))
                    .revokeAllUnrevokedByUser_Id(1L);
        }

        @Test
        @DisplayName("refresh expired - throws UnauthorizedException")
        void refreshExpired_throwsUnauthorizedException() {

            // Refresh token expired 10 hours ago
            RefreshToken expiredRefreshToken = new RefreshToken(
                    savedUser, refresh, Instant.now().minus(10, ChronoUnit.HOURS)
            );

            when(jwtTokenProvider.validateToken("some-refresh-token"))
                    .thenReturn(claims);
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(savedUser));
            when(refreshTokenRepository.findByUser_IdAndRefreshAndRevokedIsFalse(1L, "some-refresh-token"))
                    .thenReturn(Optional.of(expiredRefreshToken));

            assertThatThrownBy(() -> authService.refresh(refresh))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid refresh token.");
        }

        @Test
        @DisplayName("refresh not found in DB because revoked or invalid - throws UnauthorizedException")
        void refreshRevokedOrInvalid_throwsUnauthorizedException_becauseTokenNotFoundInDB() {

            // Behavior
            when(jwtTokenProvider.validateToken("some-refresh-token"))
                    .thenReturn(claims);
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(savedUser));
            when(refreshTokenRepository.findByUser_IdAndRefreshAndRevokedIsFalse(1L, "some-refresh-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(refresh))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Active refresh token not found.");
        }
    }

    @Nested
    @DisplayName("logout()")
    class Logout {

        private RefreshToken activeRefreshToken;
        private String refresh;

        @BeforeEach
        void setUp() {
            refresh = "some-refresh-token";

            // Unexpired & unrevoked refresh token
            activeRefreshToken = new RefreshToken(
                    savedUser, refresh, Instant.now().plus(10, ChronoUnit.HOURS)
            );
        }

        @Test
        @DisplayName("success - returns auth response")
        void success() {

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(savedUser));
            when(refreshTokenRepository.findByUser_IdAndRefreshAndRevokedIsFalse(1L, "some-refresh-token"))
                    .thenReturn(Optional.of(activeRefreshToken));

            AuthResponse response = authService.logout(1L, refresh);

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Logout Successful.");
            assertThat(response.getAccess());
            // Make sure response doesn't generate access token
            assertThat(response.getAccess()).isNull();
            assertThat(response.getUser()).isNotNull();

            verify(refreshTokenRepository, times(1))
                    .revokeAllUnrevokedByUser_Id(1L);
        }

        @Test
        @DisplayName("user not found - throws NotFoundException")
        void userNotFound_throwsNotFoundException() {

            when(userRepository.findById(1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.logout(1L, refresh))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found.");
        }

        @Test
        @DisplayName("active refresh token not found in DB - throws UnauthorizedException")
        void activeRefreshTokenNotFoundInDb_throwsUnauthorizedException() {

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(savedUser));

            when(refreshTokenRepository.findByUser_IdAndRefreshAndRevokedIsFalse(1L, "some-refresh-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.logout(1L, refresh))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Active refresh token not found.");
        }
    }


}
