package com.example.demo.service;

import com.example.demo.dao.UrlDao;
import com.example.demo.model.Url;
import com.google.common.hash.Hashing;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class UrlService {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_URL_LENGTH = 7;
    private static final int MAX_COLLISION_RETRIES = 3;
    private static final Random random = new Random();


    //Environment Flexibility: You can change the domain without recompiling code
    @Value("${app.domain:https://sturl/}")
    private String domain;

    @Autowired
    private UrlDao urlDao;

    public ResponseEntity<Long> getClickCount(String shortUrl) {
        // Input validation
        if (shortUrl == null || shortUrl.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            Optional<Url> urlEntity = urlDao.findById(shortUrl);

            if (urlEntity.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Url url = urlEntity.get();
            return new ResponseEntity<>(url.getClickCount(), HttpStatus.OK); // Changed to OK (200)
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Custom Base62 encoder
    private static class Base62 {
        private static final String CHARACTERS = ALPHABET;

        public static String encode(long number) {
            if (number == 0) return "0";
            StringBuilder sb = new StringBuilder();
            while (number > 0) {
                sb.insert(0, CHARACTERS.charAt((int)(number % 62)));
                number /= 62;
            }
            return sb.toString();
        }
    }

    @Transactional
    public ResponseEntity<String> getShortUrl(String longUrl) {
        // Validate URL format
        UrlValidator validator = new UrlValidator(new String[]{"http", "https"});
        if (!validator.isValid(longUrl)) {
            return ResponseEntity.badRequest().body("Invalid URL format");
        }

        // Check if URL already exists and isn't expired
        Optional<Url> existingUrl = urlDao.findByOriginalUrl(longUrl);
        if (existingUrl.isPresent() && !existingUrl.get().isExpired()) {
            return ResponseEntity.ok(domain + existingUrl.get().getShortUrl());
        }

        // Generate and save new short URL
        int attempts = 0;
        while (attempts < MAX_COLLISION_RETRIES) {
            String shortUrl = generateShortUrl(longUrl, attempts);

            try {
                // Direct instantiation instead of builder
                Url url = new Url();
                url.setShortUrl(shortUrl);
                url.setOriginalUrl(longUrl);
                url.setExpiresAt(LocalDateTime.now().plusYears(1)); // Expires after 1 year
                url.setClickCount(0L); // Initialize click count
                url.setCreatedAt(LocalDateTime.now()); // Set creation time

                urlDao.save(url);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(domain + shortUrl);
            } catch (Exception e) {
                attempts++;
                if (attempts == MAX_COLLISION_RETRIES) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to generate unique URL");
                }
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error during URL shortening");
    }
    private String generateShortUrl(String longUrl, int attempt) {
        String hash = Hashing.sha256()
                .hashString(longUrl + System.nanoTime(), StandardCharsets.UTF_8)
                .toString();

        // Use first 12 characters (6 bytes) of hash
        long decimalValue = Long.parseLong(hash.substring(0, 12), 16);
        String shortUrl = Base62.encode(decimalValue);

        // Ensure correct length
        if (shortUrl.length() < SHORT_URL_LENGTH) {
            shortUrl = String.format("%-" + SHORT_URL_LENGTH + "s", shortUrl)
                    .replace(' ', '0');
        } else {
            shortUrl = shortUrl.substring(0, SHORT_URL_LENGTH);
        }

        // Add attempt-specific suffix if needed
        if (attempt > 0) {
            shortUrl += "-" + randomSuffix(2);
        }

        return shortUrl;
    }

    @Transactional
    public ResponseEntity<String> getOriginalUrl(String shortUrl) {
        Optional<Url> urlEntity = urlDao.findById(shortUrl);

        if (urlEntity.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Short URL not found");
        }

        Url url = urlEntity.get();

        // Check if URL is expired
        if (url.isExpired()) {
            urlDao.delete(url);
            return ResponseEntity.status(HttpStatus.GONE)
                    .body("Short URL has expired");
        }

        // Update click count and last accessed time
        url.incrementClickCount();
        urlDao.save(url);

        return ResponseEntity.ok(url.getOriginalUrl());
    }

    private String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char)('a' + random.nextInt(26)));
        }
        return sb.toString();
    }
}