package com.tradetwin.order.model;

import java.time.Instant;

/**
 * Immutable audit record tracking a state machine transition on an order.
 */
public class OrderStatusChange {

    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String updatedBy;
    private String notes;
    private Instant timestamp = Instant.now();

    public OrderStatusChange() {}

    public OrderStatusChange(OrderStatus fromStatus, OrderStatus toStatus, String updatedBy, String notes, Instant timestamp) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.updatedBy = updatedBy;
        this.notes = notes;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public OrderStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(OrderStatus fromStatus) { this.fromStatus = fromStatus; }

    public OrderStatus getToStatus() { return toStatus; }
    public void setToStatus(OrderStatus toStatus) { this.toStatus = toStatus; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
