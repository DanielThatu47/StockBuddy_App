// src/main/java/com/stockbuddy/repository/DemoTradingAccountRepository.java
package com.stockbuddy.repository;

import com.stockbuddy.model.DemoTradingAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemoTradingAccountRepository extends MongoRepository<DemoTradingAccount, String> {

    Optional<DemoTradingAccount> findByUserId(String userId);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}
