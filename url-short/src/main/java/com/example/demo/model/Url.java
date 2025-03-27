package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "urls", indexes = {
        @Index(name = "idx_short_url", columnList = "shortUrl", unique = true),
        @Index(name = "idx_original_url", columnList = "originalUrl")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Url {

    @Id
    @Column(length = 10)  // Limits short URL to 10 chars max
    private String shortUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long clickCount = 0L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastAccessedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusYears(1); // Default 1 year expiry

    // Method to check if URL is expired
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Method to check if URL should be cleaned up (no clicks in 1 year)
    public boolean shouldCleanup() {
        return clickCount == 0 && createdAt.isBefore(LocalDateTime.now().minusYears(1));
    }

    // Method to increment clicks and update access time
    public void incrementClickCount() {
        this.clickCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}