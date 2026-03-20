package com.asiandoor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Transfers product data between the service layer and the view / API layer,
 * keeping the JPA entity out of presentation concerns.
 */
@Data
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Product name is required.")
    private String name;

    @NotBlank(message = "Category is required.")
    private String category;

    @NotBlank(message = "Material is required.")
    private String material;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.01", message = "Price must be greater than Tk 0.")
    private Double price;

    private String dimensions;

    private String lockSystem;

    @NotNull(message = "Stock is required.")
    @Min(value = 0, message = "Stock cannot be negative.")
    private Integer stock;

    private String imageUrl;

    private String description;

    private Double averageRating = 0.0;

    private Long totalReviews = 0L;
}
