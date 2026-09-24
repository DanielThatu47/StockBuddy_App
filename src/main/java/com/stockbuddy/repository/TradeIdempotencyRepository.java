package com.stockbuddy.repository;

import com.stockbuddy.model.TradeIdempotency;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradeIdempotencyRepository extends MongoRepository<TradeIdempotency, String> {

    Optional<TradeIdempotency> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}