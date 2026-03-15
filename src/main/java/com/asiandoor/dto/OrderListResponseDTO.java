package com.asiandoor.dto;

import java.util.List;

import lombok.Data;

@Data
public class OrderListResponseDTO {

    private List<OrderDTO> orders;
}
