package com.example.demo.dao;

import com.example.demo.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UrlDao extends JpaRepository<Url, String> {

    // Find by original URL (exact match)
    Optional<Url> findByOriginalUrl(String originalUrl);

    // Find all expired URLs
    @Query("SELECT u FROM Url u WHERE u.expiresAt < CURRENT_TIMESTAMP")
    List<Url> findAllExpired();

    // Find all inactive URLs (no clicks for 1 year)
    @Query("SELECT u FROM Url u WHERE u.clickCount = 0 AND u.createdAt < :cutoffDate")
    List<Url> findAllInactive(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Delete expired URLs
    @Modifying
    @Query("DELETE FROM Url u WHERE u.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpired();

    // Delete inactive URLs (no clicks for 1 year)
    @Modifying
    @Query("DELETE FROM Url u WHERE u.clickCount = 0 AND u.createdAt < :cutoffDate")
    int deleteInactive(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Check if short URL exists (alternative to findById().isPresent())
    boolean existsByShortUrl(String shortUrl);

    // Find URLs that will expire soon (for notification purposes)
    @Query("SELECT u FROM Url u WHERE u.expiresAt BETWEEN CURRENT_TIMESTAMP AND :expirationThreshold")
    List<Url> findUrlsExpiringSoon(@Param("expirationThreshold") LocalDateTime expirationThreshold);

    // Bulk update click counts (if needed)
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortUrl = :shortUrl")
    void incrementClickCount(@Param("shortUrl") String shortUrl);
}