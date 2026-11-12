package com.tradetwin.event.model;

import java.time.Instant;

public class NegotiationStreamEvent {
    private String eventType;
    private String negotiationId;
    private int turnNumber;
    private String speaker;
    private Object payload;
    private Instant timestamp = Instant.now();

    public NegotiationStreamEvent() {}

    public NegotiationStreamEvent(String eventType, String negotiationId, int turnNumber, String speaker, Object payload, Instant timestamp) {
        this.eventType = eventType;
        this.negotiationId = negotiationId;
        this.turnNumber = turnNumber;
        this.speaker = speaker;
        this.payload = payload;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String eventType;
        private String negotiationId;
        private int turnNumber;
        private String speaker;
        private Object payload;
        private Instant timestamp = Instant.now();

        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder negotiationId(String negotiationId) { this.negotiationId = negotiationId; return this; }
        public Builder turnNumber(int turnNumber) { this.turnNumber = turnNumber; return this; }
        public Builder speaker(String speaker) { this.speaker = speaker; return this; }
        public Builder payload(Object payload) { this.payload = payload; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public NegotiationStreamEvent build() {
            return new NegotiationStreamEvent(eventType, negotiationId, turnNumber, speaker, payload, timestamp);
        }
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getNegotiationId() { return negotiationId; }
    public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
