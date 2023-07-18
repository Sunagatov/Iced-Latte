package com.zufar.onlinestore.review.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {

    private UUID id;

    @NotBlank(message = "Review text is mandatory")
    @Size(min = 30, message = "Review text length must be more than 30 characters")
    @Size(max = 200, message = "Review text length must be less than 200 characters")
    private String text;

    @Min(value = 1, message = "Rating should be at least 1")
    @Max(value = 5, message = "Rating should not exceed 5")
    @NotNull(message = "Rating is mandatory")
    private int rating;

    @NotBlank(message = "Product ID is mandatory")
    private String productId;

    @NotBlank(message = "Customer ID is mandatory")
    private String customerId;
}