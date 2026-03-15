package com.asiandoor.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrderDTO {

    private Long id;
    private LocalDateTime orderDate;
    private Double totalPrice;
    private String status;
    private Long userId;
}
