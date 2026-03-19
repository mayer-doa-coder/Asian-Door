package com.asiandoor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.asiandoor.dto.CreateProductReviewRequestDTO;
import com.asiandoor.dto.ProductReviewDTO;
import com.asiandoor.dto.ProductReviewSummaryDTO;
import com.asiandoor.service.ProductReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping
    public ResponseEntity<ProductReviewSummaryDTO> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(productReviewService.getReviewSummary(productId));
    }

    @PostMapping
    public ResponseEntity<ProductReviewDTO> createReview(@PathVariable Long productId,
                                                         @RequestBody CreateProductReviewRequestDTO request,
                                                         Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ProductReviewDTO created = productReviewService.addReview(
                productId,
                authentication.getName(),
                request.getRating(),
                request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
