package com.asiandoor.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ApiErrorResponseDTO {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
