package com.tradetwin.order.repository;

import com.tradetwin.order.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByNegotiationId(String negotiationId);
    List<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId);
    List<Order> findBySellerIdOrderByCreatedAtDesc(String sellerId);
}
