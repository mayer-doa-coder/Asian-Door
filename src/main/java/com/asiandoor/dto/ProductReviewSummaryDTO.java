package com.asiandoor.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ProductReviewSummaryDTO {
    private Double averageRating;
    private Long totalReviews;
    private List<ProductReviewDTO> reviews = new ArrayList<>();
}
