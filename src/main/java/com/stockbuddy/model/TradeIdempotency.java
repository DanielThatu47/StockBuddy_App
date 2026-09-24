package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "trade_idempotency")
@CompoundIndex(name = "user_idempotency_key", def = "{'userId': 1, 'idempotencyKey': 1}", unique = true)
public class TradeIdempotency {

    @Id
    private String id;

    private String userId;

    private String idempotencyKey;

    private String status; // PROCESSING or COMPLETED

    private String symbol;

    private String type;

    private int quantity;

    private double price;

    private Date createdAt = new Date();

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
}