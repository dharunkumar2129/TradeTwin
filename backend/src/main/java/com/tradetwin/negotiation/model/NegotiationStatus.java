package com.tradetwin.negotiation.model;

public enum NegotiationStatus {
    CREATED,
    DISCOVERING,
    NEGOTIATING,
    OFFER_RECEIVED,
    COUNTEROFFERED,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    COMPLETED,

    // Aliases preserved for backwards compatibility with existing UI & tests
    INITIATED,
    IN_PROGRESS,
    AGREED,
    CANCELLED;

    public boolean isTerminal() {
        return this == ACCEPTED || this == AGREED || this == REJECTED || this == EXPIRED || this == CANCELLED || this == COMPLETED;
    }

    public boolean isSuccess() {
        return this == ACCEPTED || this == AGREED || this == COMPLETED;
    }
}
