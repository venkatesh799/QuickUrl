package com.example.demo.controller;

import com.example.demo.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:8000")
@RestController
@RequestMapping("/")
public class UrlController {
    private static final Logger logger = LoggerFactory.getLogger(UrlController.class);

    private final UrlService urlService;

    @Autowired
    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/url")
    public ResponseEntity<?> createShortUrl(@RequestBody Map<String, String> requestBody) {
        try {
            String longUrl = requestBody.get("longUrl");
            if (longUrl == null || longUrl.isBlank()) {
                return ResponseEntity.badRequest().body("Long URL is required");
            }

            logger.info("Creating short URL for: {}", longUrl);
            ResponseEntity<String> response = urlService.getShortUrl(longUrl);

            // Log the result
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully created short URL: {}", response.getBody());
            } else {
                logger.warn("Failed to create short URL. Status: {}", response.getStatusCode());
            }

            return response;

        } catch (Exception e) {
            logger.error("Error creating short URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing your request");
        }
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> redirectToOriginalUrl(@PathVariable String shortUrl) {
        try {
            if (shortUrl == null || shortUrl.isBlank()) {
                return ResponseEntity.badRequest().body("Short URL is required");
            }

            logger.debug("Redirecting short URL: {}", shortUrl);
            ResponseEntity<String> response = urlService.getOriginalUrl(shortUrl);

            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.warn("Short URL not found: {}", shortUrl);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Short URL not found or expired");
            }

            return response;

        } catch (Exception e) {
            logger.error("Error redirecting short URL: {}", shortUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing your request");
        }
    }

    @GetMapping("clicks/{shorturl}")
    public ResponseEntity<Long> getClickCount(@PathVariable String shorturl) {
        return urlService.getClickCount(shorturl);
    }
}