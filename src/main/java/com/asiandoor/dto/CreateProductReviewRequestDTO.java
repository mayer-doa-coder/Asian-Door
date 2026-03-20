package com.asiandoor.dto;

import lombok.Data;

@Data
public class CreateProductReviewRequestDTO {
    private Integer rating;
    private String comment;
}
