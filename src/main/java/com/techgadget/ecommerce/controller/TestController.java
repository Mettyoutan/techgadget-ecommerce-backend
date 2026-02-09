package com.techgadget.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for testing only
 */
@RestController
@RequestMapping("/test")
@Validated
public class TestController {

    /**
     * Public endpoint (no auth required)
     */
    @GetMapping("/public")
    public ResponseEntity<?> testPublic() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "This is a public endpoint");
        return ResponseEntity.ok(response);
    }

    /**
     * Protected endpoint (auth required)
     */
    @GetMapping("/protected")
    public ResponseEntity<?> testProtected() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "This is a protected endpoint");
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("username", authentication.getName());  // email in this case
            response.put("authorities", authentication.getAuthorities());
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "No user authenticated");
        return ResponseEntity.status(401).body(response);
    }
}
