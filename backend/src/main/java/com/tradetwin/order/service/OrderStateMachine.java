package com.tradetwin.order.service;

import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.order.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Controlled Order State Machine:
 * Ensures order statuses accurately reflect real payment and fulfilment actions.
 *
 * Valid Transitions:
 * PAYMENT_PENDING -> PAID, CANCELLED
 * PAID            -> CONFIRMED, REFUNDED
 * CONFIRMED       -> PROCESSING, CANCELLED, REFUNDED
 * PROCESSING      -> SHIPPED, CANCELLED, REFUNDED
 * SHIPPED         -> DELIVERED, REFUNDED
 * DELIVERED       -> REFUNDED
 * CANCELLED       -> [terminal]
 * REFUNDED        -> [terminal]
 */
@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PAYMENT_PENDING, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.CONFIRMED, OrderStatus.REFUNDED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED, OrderStatus.REFUNDED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED, OrderStatus.REFUNDED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.REFUNDED, Set.of()
    );

    public void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return; // Idempotent no-op
        }

        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessRuleException(
                    String.format("Invalid order state transition: Cannot change status from [%s] to [%s]. Allowed transitions: %s",
                            currentStatus, targetStatus, allowed)
            );
        }
    }

    public boolean canTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (currentStatus == targetStatus) return true;
        return VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }
}
