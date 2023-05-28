package com.zufar.onlinestore.review.controller;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiResponse<T> {
    private T data;
    private String message;
    private int status;
    private LocalDateTime timeStamp;
}