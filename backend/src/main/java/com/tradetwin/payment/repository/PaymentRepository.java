package com.tradetwin.payment.repository;

import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<PaymentRecord, String> {
    Optional<PaymentRecord> findByPaypalOrderId(String paypalOrderId);
    Optional<PaymentRecord> findByNegotiationId(String negotiationId);
    Optional<PaymentRecord> findByCaptureId(String captureId);
    List<PaymentRecord> findByBuyerIdAndStatus(String buyerId, PaymentStatus status);
}
