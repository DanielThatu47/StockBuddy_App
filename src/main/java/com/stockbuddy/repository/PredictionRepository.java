// src/main/java/com/stockbuddy/repository/PredictionRepository.java
package com.stockbuddy.repository;

import com.stockbuddy.model.Prediction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends MongoRepository<Prediction, String> {

    List<Prediction> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Prediction> findByTaskId(String taskId);

    Optional<Prediction> findByUserIdAndSymbolAndStatusIn(String userId, String symbol, List<String> statuses);

    List<Prediction> findByIdInAndUserId(List<String> ids, String userId);

    void deleteByUserId(String userId);

    long deleteByUserIdAndIdIn(String userId, List<String> ids);
}
