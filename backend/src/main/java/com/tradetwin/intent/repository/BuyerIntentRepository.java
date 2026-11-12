package com.tradetwin.intent.repository;

import com.tradetwin.intent.model.BuyerIntent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuyerIntentRepository extends MongoRepository<BuyerIntent, String> {
    List<BuyerIntent> findByBuyerIdOrderByCreatedAtDesc(String buyerId);
    Optional<BuyerIntent> findByIdAndBuyerId(String id, String buyerId);
}
