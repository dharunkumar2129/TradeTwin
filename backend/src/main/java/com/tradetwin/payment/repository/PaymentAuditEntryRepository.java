package com.tradetwin.payment.repository;

import com.tradetwin.payment.model.PaymentAuditEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAuditEntryRepository extends MongoRepository<PaymentAuditEntry, String> {
    List<PaymentAuditEntry> findByBuyerIdOrderByUpdatedAtDesc(String buyerId);
    Optional<PaymentAuditEntry> findByNegotiationId(String negotiationId);
}
