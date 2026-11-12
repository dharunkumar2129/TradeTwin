package com.tradetwin.order.model;

/**
 * Controlled Order State Machine Statuses:
 * PAYMENT_PENDING -> Initial state before payment capture authorization
 * PAID            -> PayPal payment captured and verified on server side
 * CONFIRMED       -> Deal sealed, stock deducted, and order confirmed
 * PROCESSING      -> Merchant packing and preparing order
 * SHIPPED         -> Handed over to shipping carrier with tracking number
 * DELIVERED       -> Delivered to destination address
 * CANCELLED       -> Cancelled prior to delivery (stock restocked)
 * REFUNDED        -> Payment refunded (stock restocked)
 */
public enum OrderStatus {
    PAYMENT_PENDING,
    PAID,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
