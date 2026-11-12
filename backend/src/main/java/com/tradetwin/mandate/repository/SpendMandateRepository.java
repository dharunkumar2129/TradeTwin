package com.tradetwin.mandate.repository;

import com.tradetwin.mandate.model.MandateStatus;
import com.tradetwin.mandate.model.SpendMandate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpendMandateRepository extends MongoRepository<SpendMandate, String> {
    List<SpendMandate> findByBuyerIdOrderByUpdatedAtDesc(String buyerId);
    Optional<SpendMandate> findFirstByBuyerIdAndStatusOrderByUpdatedAtDesc(String buyerId, MandateStatus status);
}
