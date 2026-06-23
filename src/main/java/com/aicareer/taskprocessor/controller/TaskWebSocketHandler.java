package com.aicareer.taskprocessor.controller;

import com.aicareer.taskprocessor.service.TaskUpdateBroadcaster;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TaskWebSocketHandler extends TextWebSocketHandler {

    private final TaskUpdateBroadcaster broadcaster;

    public TaskWebSocketHandler(TaskUpdateBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.remove(session);
    }
}
