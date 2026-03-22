package com.asiandoor.dto;

import lombok.Data;

@Data
public class OrderCreateRequestDTO {

    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String deliveryAddress;
    private String paymentType;
}
