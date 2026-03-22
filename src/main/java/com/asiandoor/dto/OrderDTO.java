package com.asiandoor.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderDTO {

    private Long id;
    private LocalDateTime orderDate;
    private Double totalPrice;
    private String status;
    private Long userId;
    private String paymentType;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String deliveryAddress;
    private List<OrderItemSummaryDTO> items;
    private String orderDateDisplay;
    private String orderTimeDisplay;
}
