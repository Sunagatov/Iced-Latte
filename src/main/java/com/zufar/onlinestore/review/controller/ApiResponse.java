package com.zufar.onlinestore.review.controller;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Objects;

@Builder
public class ApiResponse<T> {
    private T data;
    private String message;
    private int status;
    private LocalDateTime timeStamp;

    public T getData() {
        return this.data;
    }

    public String getMessage() {
        return this.message;
    }

    public int getStatus() {
        return this.status;
    }

    public LocalDateTime getTimeStamp() {
        return this.timeStamp;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;if (o == null || getClass() != o.getClass()) return false;
        ApiResponse<?> that = (ApiResponse<?>) o;
        return status == that.status && data.equals(that.data) && message.equals(that.message) && timeStamp.equals(that.timeStamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, message, status, timeStamp);
    }

    public String toString() {
        return "ApiResponse(data=" + this.getData() + ", message=" + this.getMessage() + ", status=" + this.getStatus() + ", timeStamp=" + this.getTimeStamp() + ")";
    }
}