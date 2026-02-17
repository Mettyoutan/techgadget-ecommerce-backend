package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Get non revoked user refresh token
     */
    Optional<RefreshToken> findByUser_IdAndRefreshAndRevokedIsFalse(Long userId, String refresh);
}
