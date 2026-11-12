package com.tradetwin.order.dto;

import com.tradetwin.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateOrderStatusRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    private String carrier;
    private String trackingNumber;
    private String notes;

    public UpdateOrderStatusRequest() {}

    public UpdateOrderStatusRequest(OrderStatus status, String carrier, String trackingNumber, String notes) {
        this.status = status;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.notes = notes;
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
