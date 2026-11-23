package com.fintech.notification.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire WebSocket pour le canal temps réel /ws/tx (Tâche P1.7).
 * Diffuse les événements TX_DECIDED, QUEUE_UPDATED et POD_EVENT au Frontend React.
 * Conforme à la spécification docs/contracts/websocket-events.json v1.1.0.
 */
@Component
public class TransactionWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TransactionWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Client WebSocket connecté [sessionId={}, totalClients={}]", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Client WebSocket déconnecté [sessionId={}, status={}, totalClients={}]", session.getId(), status, sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Message reçu du client {}: {}", session.getId(), payload);
        // Ping/Pong ou acquittement de réception
        if ("ping".equalsIgnoreCase(payload.trim())) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Erreur de transport WebSocket sur la session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    /**
     * Diffuse un événement JSON à l'ensemble des clients Frontend connectés.
     */
    public synchronized void broadcast(String jsonPayload) {
        if (sessions.isEmpty()) {
            log.debug("Aucun client WebSocket connecté pour diffuser l'événement");
            return;
        }

        TextMessage textMessage = new TextMessage(jsonPayload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Échec d'envoi du message au client {}: {}", session.getId(), e.getMessage());
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
        log.debug("Événement diffusé avec succès à {} clients actifs", sessions.size());
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}
