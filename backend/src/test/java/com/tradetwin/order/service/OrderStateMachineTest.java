package com.tradetwin.order.service;

import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    @Test
    @DisplayName("Valid forward order transitions are allowed")
    void testValidForwardTransitions() {
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.PAYMENT_PENDING, OrderStatus.PAID));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.PAID, OrderStatus.CONFIRMED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.PROCESSING, OrderStatus.SHIPPED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.DELIVERED, OrderStatus.REFUNDED));
    }

    @Test
    @DisplayName("Backward and invalid state jumps are rejected")
    void testInvalidStateTransitionsThrowException() {
        // Cannot jump from PAYMENT_PENDING directly to SHIPPED
        assertThrows(BusinessRuleException.class,
                () -> stateMachine.validateTransition(OrderStatus.PAYMENT_PENDING, OrderStatus.SHIPPED));

        // Cannot move backwards from SHIPPED to PROCESSING
        assertThrows(BusinessRuleException.class,
                () -> stateMachine.validateTransition(OrderStatus.SHIPPED, OrderStatus.PROCESSING));

        // Cannot transition out of terminal CANCELLED state
        assertThrows(BusinessRuleException.class,
                () -> stateMachine.validateTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED));

        // Cannot transition out of terminal REFUNDED state
        assertThrows(BusinessRuleException.class,
                () -> stateMachine.validateTransition(OrderStatus.REFUNDED, OrderStatus.DELIVERED));
    }
}
