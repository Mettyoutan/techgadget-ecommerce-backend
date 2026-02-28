package com.techgadget.ecommerce.unit_test;

import com.techgadget.ecommerce.dto.request.auth.LoginRequest;
import com.techgadget.ecommerce.dto.request.auth.RegisterRequest;
import com.techgadget.ecommerce.dto.response.auth.AuthServiceResponse;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.enums.UserRole;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.exception.UnauthorizedException;
import com.techgadget.ecommerce.repository.RefreshTokenRepository;
import com.techgadget.ecommerce.repository.UserRepository;
import com.techgadget.ecommerce.security.JwtTokenProvider;
import com.techgadget.ecommerce.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthTest {

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

    @BeforeEach
    void setUp() {
        // Set auth service field
        ReflectionTestUtils.setField(authService, "refreshExpirationInMs", 604800000);
    }

    @Test
    void test_register_success() {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("username");
        request.setPassword("password");
        request.setEmail("email@gmail.com");
        request.setFullName("Full Name");

        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("hashed");
        Mockito.when(jwtTokenProvider.generateAccessToken(Mockito.any(), Mockito.any()))
                .thenReturn("access-token");
        Mockito.when(jwtTokenProvider.generateRefreshToken(Mockito.any(), Mockito.any()))
                .thenReturn("refresh-token");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername(request.getUsername());
        savedUser.setPassword(request.getPassword());
        savedUser.setEmail(request.getEmail());
        savedUser.setFullName(request.getFullName());
        savedUser.setRole(UserRole.CUSTOMER);
        savedUser.setPhoneNumber(null);
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());

        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(savedUser);

        AuthServiceResponse response = authService.register(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("Registration Successful.", response.getMessage());
        Assertions.assertEquals("access-token", response.getAccess());
        Assertions.assertEquals("refresh-token", response.getRefresh());

        Mockito.verify(refreshTokenRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(userRepository, Mockito.atLeastOnce()).save(Mockito.any(User.class));
    }

    @Test
    void test_register_duplicateEmailOrUsername_throwConflict() {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("username");
        request.setPassword("password");
        request.setEmail("email@gmail.com");
        request.setFullName("Full Name");

        // Duplicate will throw DataIntegrityViolationException
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenThrow(DataIntegrityViolationException.class);

        Assertions.assertThrows(ConflictException.class,
                () -> authService.register(request));
    }

    @Test
    void test_login_success() {

        LoginRequest request = new LoginRequest();
        request.setPassword("email@gmail.com");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setUsername("username");
        user.setEmail("email@gmail.com");
        user.setPassword("password");
        user.setFullName("Full Name");
        user.setRole(UserRole.CUSTOMER);

        Mockito.when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .thenReturn(true);
        Mockito.when(jwtTokenProvider.generateAccessToken(Mockito.any(), Mockito.any()))
                .thenReturn("access-token");
        Mockito.when(jwtTokenProvider.generateRefreshToken(Mockito.any(), Mockito.any()))
                .thenReturn("refresh-token");
        Mockito.when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        AuthServiceResponse response = authService.login(request);
        Assertions.assertNotNull(response);
        Assertions.assertEquals("Login Successful.", response.getMessage());
        Assertions.assertEquals("access-token", response.getAccess());
        Assertions.assertEquals("refresh-token", response.getRefresh());

        Mockito.verify(refreshTokenRepository, Mockito.atLeastOnce()).save(Mockito.any());
        Mockito.verify(userRepository, Mockito.atLeastOnce()).findByEmail(request.getEmail());
    }

    @Test
    void test_login_EmailNotFound_throwNotFound() {

        LoginRequest request = new LoginRequest();
        request.setPassword("email@gmail.com");
        request.setPassword("password");

        Mockito.when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class,
                () -> authService.login(request));
    }

    @Test
    void test_login_PasswordNotMatch_throwUnauthorized() {

        LoginRequest request = new LoginRequest();
        request.setEmail("email@gmail.com");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setEmail("email@gmail.com");
        user.setPassword("hashed");

        Mockito.when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        // When password not match
        Mockito.when(passwordEncoder.matches(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(false);

        Assertions.assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }
}
