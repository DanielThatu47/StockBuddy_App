// src/main/java/com/stockbuddy/repository/UserPreferencesRepository.java
package com.stockbuddy.repository;
import com.stockbuddy.model.UserPreferences;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends MongoRepository<UserPreferences, String> {
    Optional<UserPreferences> findByUserId(String userId);

    void deleteByUserId(String userId);
}
