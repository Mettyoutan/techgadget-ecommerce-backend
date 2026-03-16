package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Search active user refresh token
     */
    Optional<RefreshToken> findByUser_IdAndRefreshAndRevokedIsFalse(Long userId, String refresh);

    /**
     * Check if user has active refresh tokens
     */
    boolean existsByUser_IdAndRevokedIsFalse(Long userId);

    @Modifying
    @Query("""
    UPDATE RefreshToken t
    SET t.revoked = true
    WHERE t.user.id = :userId
    AND t.revoked = false
    """)
    void revokeAllUnrevokedByUser_Id(Long userId);
}
