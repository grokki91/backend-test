package com.example.backendtest.messaging;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.backendtest.order.OrderEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Fans an order event out to every connected SSE stream and WebSocket session. */
@Component
public class EventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(EventBroadcaster.class);

    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> webSocketSessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public EventBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter openSseStream(long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError(error -> sseEmitters.remove(emitter));
        sseEmitters.add(emitter);
        return emitter;
    }

    public void register(WebSocketSession session) {
        webSocketSessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        webSocketSessions.remove(session);
    }

    public int sseSubscribers() {
        return sseEmitters.size();
    }

    public int webSocketSubscribers() {
        return webSocketSessions.size();
    }

    public void broadcast(OrderEvent event) {
        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("cannot serialize event {}", event.eventId(), e);
            return;
        }
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event().id(event.eventId()).name(event.type()).data(json));
            } catch (IOException | IllegalStateException e) {
                sseEmitters.remove(emitter);
            }
        }
        for (WebSocketSession session : webSocketSessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                } else {
                    webSocketSessions.remove(session);
                }
            } catch (IOException e) {
                webSocketSessions.remove(session);
            }
        }
    }
}
