package com.asiandoor.dto;

import lombok.Data;

@Data
public class OrderCreateResponseDTO {

    private boolean success;
    private Long orderId;
    private String status;
    private Double totalPrice;
    private String message;
}
