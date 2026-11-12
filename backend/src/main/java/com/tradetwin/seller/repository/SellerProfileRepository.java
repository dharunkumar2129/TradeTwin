package com.tradetwin.seller.repository;

import com.tradetwin.seller.model.SellerProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerProfileRepository extends MongoRepository<SellerProfile, String> {
    Optional<SellerProfile> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
