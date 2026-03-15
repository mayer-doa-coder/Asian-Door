package com.asiandoor.dto;

import java.util.List;

import lombok.Data;

@Data
public class CartSummaryDTO {

    private List<CartItemDTO> items;
    private Double totalAmount;
    private Integer itemCount;
}
