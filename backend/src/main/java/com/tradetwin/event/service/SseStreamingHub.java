package com.tradetwin.event.service;

import com.tradetwin.event.model.NegotiationEventDocument;
import com.tradetwin.event.model.NegotiationStreamEvent;
import com.tradetwin.event.repository.NegotiationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseStreamingHub {

    private static final Logger log = LoggerFactory.getLogger(SseStreamingHub.class);

    private final NegotiationEventRepository eventRepository;
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseStreamingHub(NegotiationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public SseEmitter subscribe(String negotiationId) {
        SseEmitter emitter = new SseEmitter(1800000L);

        emitters.computeIfAbsent(negotiationId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(negotiationId, emitter));
        emitter.onTimeout(() -> removeEmitter(negotiationId, emitter));
        emitter.onError(e -> removeEmitter(negotiationId, emitter));

        try {
            // First emit CONNECTED event
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(NegotiationStreamEvent.builder()
                            .eventType("CONNECTED")
                            .negotiationId(negotiationId)
                            .timestamp(Instant.now())
                            .build()));

            // Replay historical persisted events so a refreshed page instantly catches up
            List<NegotiationEventDocument> history = eventRepository.findByNegotiationIdOrderByTimestampAsc(negotiationId);
            for (NegotiationEventDocument doc : history) {
                emitter.send(SseEmitter.event()
                        .name(doc.getEventType())
                        .data(NegotiationStreamEvent.builder()
                                .eventType(doc.getEventType())
                                .negotiationId(doc.getNegotiationId())
                                .turnNumber(doc.getTurnNumber())
                                .speaker(doc.getSpeaker())
                                .payload(doc.getPayload())
                                .timestamp(doc.getTimestamp())
                                .build()));
            }
        } catch (IOException e) {
            removeEmitter(negotiationId, emitter);
        }

        return emitter;
    }

    public void broadcast(String negotiationId, NegotiationStreamEvent event) {
        // 1. Persist event to MongoDB for durable history
        try {
            NegotiationEventDocument doc = NegotiationEventDocument.builder()
                    .negotiationId(negotiationId)
                    .eventType(event.getEventType())
                    .turnNumber(event.getTurnNumber())
                    .speaker(event.getSpeaker())
                    .payload(event.getPayload())
                    .timestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
                    .build();
            eventRepository.save(doc);
        } catch (Exception ex) {
            log.error("Failed to persist negotiation event for {}: {}", negotiationId, ex.getMessage());
        }

        // 2. Broadcast live to active SSE subscribers
        List<SseEmitter> list = emitters.get(negotiationId);
        if (list == null || list.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event));
            } catch (IOException ex) {
                removeEmitter(negotiationId, emitter);
            }
        }
    }

    public List<NegotiationEventDocument> getPersistedEvents(String negotiationId) {
        return eventRepository.findByNegotiationIdOrderByTimestampAsc(negotiationId);
    }

    private void removeEmitter(String negotiationId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(negotiationId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(negotiationId);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        emitters.forEach((negotiationId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException e) {
                    removeEmitter(negotiationId, emitter);
                }
            }
        });
    }
}
