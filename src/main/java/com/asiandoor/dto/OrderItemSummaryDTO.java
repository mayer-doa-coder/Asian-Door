package com.asiandoor.dto;

import lombok.Data;

@Data
public class OrderItemSummaryDTO {

    private Long productId;
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private Double unitPrice;
    private Double lineTotal;
}
