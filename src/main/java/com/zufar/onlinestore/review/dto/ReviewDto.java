package com.zufar.onlinestore.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {


    private String id;

    @NotBlank(message = "Review text is mandatory")
    @Size(min = 30, message = "Review text length must be more than 30 characters")
    @Size(max = 200, message = "Review text length must be less than 200 characters")
    private String text;

    @Min(value = 1, message = "Rating should be at least 1")
    @Max(value = 5, message = "Rating should not exceed 5")
    @NotBlank(message = "Rating is mandatory")
    private int rating;

    private String productId;

    private String customerId;





}
