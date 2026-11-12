package com.tradetwin.event.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "negotiation_events")
@CompoundIndex(name = "neg_time_idx", def = "{'negotiationId': 1, 'timestamp': 1}")
public class NegotiationEventDocument {

    @Id
    private String id;

    @Indexed
    private String negotiationId;

    private String eventType;
    private int turnNumber;
    private String speaker;
    private Object payload;

    @Indexed
    private Instant timestamp = Instant.now();

    public NegotiationEventDocument() {}

    public NegotiationEventDocument(String id, String negotiationId, String eventType, int turnNumber,
                                    String speaker, Object payload, Instant timestamp) {
        this.id = id;
        this.negotiationId = negotiationId;
        this.eventType = eventType;
        this.turnNumber = turnNumber;
        this.speaker = speaker;
        this.payload = payload;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String negotiationId;
        private String eventType;
        private int turnNumber;
        private String speaker;
        private Object payload;
        private Instant timestamp = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder negotiationId(String negotiationId) { this.negotiationId = negotiationId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder turnNumber(int turnNumber) { this.turnNumber = turnNumber; return this; }
        public Builder speaker(String speaker) { this.speaker = speaker; return this; }
        public Builder payload(Object payload) { this.payload = payload; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public NegotiationEventDocument build() {
            return new NegotiationEventDocument(id, negotiationId, eventType, turnNumber, speaker, payload, timestamp);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNegotiationId() { return negotiationId; }
    public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
