package com.asiandoor.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ProductReviewDTO {
    private Long id;
    private Long productId;
    private String reviewerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
