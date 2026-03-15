package com.asiandoor.dto;

import lombok.Data;

@Data
public class CartActionResponseDTO {

    private boolean success;
    private String message;
    private Long productId;
    private Integer quantity;
}
