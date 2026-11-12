package com.tradetwin.event.repository;

import com.tradetwin.event.model.NegotiationEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NegotiationEventRepository extends MongoRepository<NegotiationEventDocument, String> {
    List<NegotiationEventDocument> findByNegotiationIdOrderByTimestampAsc(String negotiationId);
}
