package com.asiandoor.dto;

import lombok.Data;

/**
 * Transfers product data between the service layer and the view / API layer,
 * keeping the JPA entity out of presentation concerns.
 */
@Data
public class ProductDTO {

    private Long id;

    private String name;

    private String category;

    private String material;

    private Double price;

    private String dimensions;

    private Integer stock;

    private String imageUrl;

    private String description;
}
