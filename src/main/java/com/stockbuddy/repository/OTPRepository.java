// src/main/java/com/stockbuddy/repository/OTPRepository.java
package com.stockbuddy.repository;
import com.stockbuddy.model.OTPRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OTPRepository extends MongoRepository<OTPRecord, String> {
    Optional<OTPRecord> findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(String userId, String purpose);
    void deleteByUserIdAndPurpose(String userId, String purpose);
}
