// src/main/java/com/stockbuddy/repository/SessionRepository.java
package com.stockbuddy.repository;
import com.stockbuddy.model.UserSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends MongoRepository<UserSession, String> {
    List<UserSession> findByUserIdAndIsActiveTrueOrderByLastActiveDesc(String userId);
    Optional<UserSession> findByUserIdAndIsCurrentTrueAndIsActiveTrue(String userId);
    List<UserSession> findByUserId(String userId);
    Optional<UserSession> findByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}
