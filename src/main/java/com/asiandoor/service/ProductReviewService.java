package com.asiandoor.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.asiandoor.dto.ProductReviewDTO;
import com.asiandoor.dto.ProductReviewSummaryDTO;
import com.asiandoor.entity.Product;
import com.asiandoor.entity.ProductReview;
import com.asiandoor.entity.User;
import com.asiandoor.exception.ResourceNotFoundException;
import com.asiandoor.repository.ProductRepository;
import com.asiandoor.repository.ProductReviewRepository;
import com.asiandoor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductReviewSummaryDTO getReviewSummary(Long productId) {
        ProductReviewSummaryDTO summary = new ProductReviewSummaryDTO();

        List<ProductReviewDTO> reviews = productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        Double average = productReviewRepository.findAverageRatingByProductId(productId);
        long count = productReviewRepository.countByProductId(productId);

        summary.setAverageRating(average != null ? average : 0.0);
        summary.setTotalReviews(count);
        summary.setReviews(reviews);
        return summary;
    }

    public ProductReviewDTO addReview(Long productId, String userEmail, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (!StringUtils.hasText(comment)) {
            throw new IllegalArgumentException("Review text is required.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setReviewerName(user.getName());
        review.setRating(rating);
        review.setComment(comment.trim());

        return toDTO(productReviewRepository.save(review));
    }

    private ProductReviewDTO toDTO(ProductReview review) {
        ProductReviewDTO dto = new ProductReviewDTO();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setReviewerName(review.getReviewerName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
