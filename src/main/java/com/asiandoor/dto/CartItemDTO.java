package com.asiandoor.dto;

import lombok.Data;

@Data
public class CartItemDTO {

    private Long productId;
    private String productName;
    private String imageUrl;
    private Double unitPrice;
    private Integer quantity;
    private Double lineTotal;
}
