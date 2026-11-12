package com.tradetwin.negotiation.repository;

import com.tradetwin.negotiation.model.Negotiation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NegotiationRepository extends MongoRepository<Negotiation, String> {
    List<Negotiation> findByBuyerIdOrderByCreatedAtDesc(String buyerId);
    List<Negotiation> findBySellerIdOrderByCreatedAtDesc(String sellerId);
    List<Negotiation> findByProductId(String productId);
}
