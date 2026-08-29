package com.example.backendtest.messaging;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Broadcast-only socket at /ws/orders; anything sent to it is echoed back. */
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final EventBroadcaster broadcaster;

    public OrderWebSocketHandler(EventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        broadcaster.register(session);
        session.sendMessage(new TextMessage("{\"type\":\"connected\",\"sessionId\":\"" + session.getId() + "\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        session.sendMessage(new TextMessage("{\"type\":\"echo\",\"payload\":" + quote(message.getPayload()) + "}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.unregister(session);
    }

    private String quote(String raw) {
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
