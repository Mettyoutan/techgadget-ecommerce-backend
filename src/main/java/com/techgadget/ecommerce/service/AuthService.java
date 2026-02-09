package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.request.LoginRequest;
import com.techgadget.ecommerce.dto.request.RegisterRequest;
import com.techgadget.ecommerce.dto.response.AuthResponse;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.exception.ConflictException;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.exception.UnauthorizedException;
import com.techgadget.ecommerce.repository.UserRepository;
import com.techgadget.ecommerce.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Register request - email={}, username={}", request.getEmail(), request.getUsername());

        // Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(hashedPassword);
        user.setFullName(request.getFullName());

        // Check duplicate and save
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email or Username already registered.");
        }

        // Generate token
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail());

        log.info("User registered: id={}", user.getId());

        // Build response
        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
        );

        return new AuthResponse("Registration Successful.", token, userDto);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.debug("Login request: email={}", request.getEmail());

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

        // Generate token
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail());

        log.info("User logged in: id={}", user.getId());

        // Build response
        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
        );

        return new AuthResponse("Login Successful.", token, userDto);
    }
}
