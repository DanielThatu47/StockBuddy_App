// src/main/java/com/stockbuddy/repository/WatchlistEntryRepository.java
package com.stockbuddy.repository;

import com.stockbuddy.model.WatchlistEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistEntryRepository extends MongoRepository<WatchlistEntry, String> {

	List<WatchlistEntry> findByUserIdOrderByCreatedAtDesc(String userId);

	Optional<WatchlistEntry> findByUserIdAndSymbol(String userId, String symbol);

	void deleteByUserIdAndSymbol(String userId, String symbol);

	void deleteByUserId(String userId);
}
