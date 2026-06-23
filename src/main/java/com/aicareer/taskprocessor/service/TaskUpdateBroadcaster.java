package com.aicareer.taskprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskUpdateBroadcaster {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public TaskUpdateBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void add(WebSocketSession session) {
        sessions.add(session);
    }

    public void remove(WebSocketSession session) {
        sessions.remove(session);
    }

    public void broadcast(Object payload) {
        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(payload));
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    send(session, message);
                }
            }
        } catch (IOException ignored) {
            // A dashboard refresh will recover from transient WebSocket serialization issues.
        }
    }

    private void send(WebSocketSession session, TextMessage message) {
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException ex) {
            sessions.remove(session);
        }
    }
}
