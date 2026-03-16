package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.request.auth.LoginRequest;
import com.techgadget.ecommerce.dto.request.auth.RegisterRequest;
import com.techgadget.ecommerce.dto.response.auth.AuthResponse;
import com.techgadget.ecommerce.dto.response.auth.AuthServiceResponse;
import com.techgadget.ecommerce.dto.response.user.UserResponse;
import com.techgadget.ecommerce.entity.RefreshToken;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.exception.UnauthorizedException;
import com.techgadget.ecommerce.repository.RefreshTokenRepository;
import com.techgadget.ecommerce.repository.UserRepository;
import com.techgadget.ecommerce.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationInMs;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthServiceResponse register(RegisterRequest request) {
        log.debug("Processing register request - Email: {}, Username: {}",
                request.getEmail(), request.getUsername());

        // Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        log.debug("Password hashed successfully");

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(hashedPassword);
        user.setFullName(request.getFullName());

        // Check duplicate and save
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email or Username already registered.");
        }

        // Generate tokens
        String access = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail());
        String refresh = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail());

        // Add refresh to repo
        addRefreshToRepository(refresh, user);

        log.info("User {} registered successfully", user.getId());

        // Build response
        UserResponse userRes = mapToUserResponse(user);

        return new AuthServiceResponse(
                "Registration Successful.",
                access,
                refresh,
                userRes
        );
    }

    @Transactional
    public AuthServiceResponse login(LoginRequest request) {
        log.debug("Processing login request - Email: {}", request.getEmail());

        // Check existing user
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // If user not found
        if (user == null) {
            log.warn("Login failed - email not found: email={}", request.getEmail());
            throw new NotFoundException("Invalid username or password.");
        }

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - password not match: email={}", request.getEmail());
            throw new UnauthorizedException("Invalid username or password.");
        }

        // (For Safety) Revoke all active refresh token from user
        refreshTokenRepository.revokeAllUnrevokedByUser_Id(user.getId());

        // TODO:
//        // Make sure user doesn't have any active refresh token
//        boolean isActiveRefreshExists = refreshTokenRepository
//                .existsByUser_IdAndRevokedIsFalse(user.getId()); // Check if user has any active refresh token
//        if (isActiveRefreshExists) {
//            throw new UnauthorizedException("")
//        }

        // Generate token
        String access = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail());
        String refresh = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail());

        // Add refresh to repo
        addRefreshToRepository(refresh, user);

        log.info("User {} logged in successfully", user.getId());

        // Build response
        UserResponse userRes = mapToUserResponse(user);

        return new AuthServiceResponse(
                "Login Successful.",
                access,
                refresh,
                userRes
        );
    }

    @Transactional
    public AuthServiceResponse refresh(String refresh) {
        log.debug("Processing refresh request.");

        // Validate token & parse claims
        Claims claims = jwtTokenProvider.validateToken(refresh);

        Long userId = claims.getSubject() != null
                ? Long.parseLong(claims.getSubject())
                : null;
        String email = claims.get("email", String.class);

        // Make sure Jwt claims exists
        if (userId == null) {
            log.warn("Refresh failed - User not found on refresh");
            throw new UnauthorizedException("Invalid refresh token.");
        }

        if (email == null) {
            log.warn("Refresh failed - Email not found on refresh");
            throw new UnauthorizedException("Invalid refresh token.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User {} not found", userId);
                    return new NotFoundException("User not found.");
                });

        // Find requested refresh token & check if it's unrevoked
        RefreshToken curRefresh = refreshTokenRepository
                .findByUser_IdAndRefreshAndRevokedIsFalse(userId, refresh)
                .orElseThrow(() -> {
                    log.warn("Unrevoked refresh token not found for user {}", userId);
                    return new UnauthorizedException("Active refresh token not found.");
                });

        // Check if refresh is expired
        if (curRefresh.isExpired()) {
            log.warn("Refresh token expired for user {}", userId);
            throw new UnauthorizedException("Invalid refresh token.");
        }

        // (FOR SAFETY) Revoked all active token from user
        refreshTokenRepository.revokeAllUnrevokedByUser_Id(userId);

        // Create new access and refresh
        String access = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail());
        String newRefresh = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail());

        // Add refresh to repo
        addRefreshToRepository(newRefresh, user);

        // Build response
        UserResponse userRes = mapToUserResponse(user);

        return new AuthServiceResponse(
                "Refresh Successful.",
                access,
                newRefresh,
                userRes
        );
    }

    @Transactional
    public AuthResponse logout(Long userId, String refresh) {
        log.debug("Processing logout request - User: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User {} not found", userId);
                    return new NotFoundException("User not found.");
                });

        RefreshToken refreshToken = refreshTokenRepository
                .findByUser_IdAndRefreshAndRevokedIsFalse(userId, refresh)
                .orElseThrow(() -> {
                    log.warn("Unrevoked refresh token not found - User: {}", userId);
                    return new UnauthorizedException("Active refresh token not found.");
                });

        // (For Safety) Revoke all active refresh tokens
        refreshTokenRepository.revokeAllUnrevokedByUser_Id(userId);

        UserResponse userRes = mapToUserResponse(user);

        return new AuthResponse(
                "Logout Successful.",
                null,
                userRes
        );
    }

    private void addRefreshToRepository(String refresh, User user) {

        Instant expiryDate = Instant.now().plusMillis(refreshExpirationInMs);

        RefreshToken refreshToken = new RefreshToken(
                user, refresh, expiryDate);

        refreshToken = refreshTokenRepository.save(refreshToken);

        log.debug("User {} successfully added refresh token to repository",
                user.getId());
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
        );
    }
}
