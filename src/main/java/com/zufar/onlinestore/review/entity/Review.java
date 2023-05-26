package com.zufar.onlinestore.review.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "review")
public class Review {
    @Id
    private String id;

    private String productId;
    private String customerId;
    private String text;
    private int rating;

    @CreatedDate
    @Field("date")
    private Date date;
}
