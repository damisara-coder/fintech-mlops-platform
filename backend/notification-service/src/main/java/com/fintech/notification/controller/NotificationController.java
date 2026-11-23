package com.fintech.notification.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.notification.dto.NotificationEvent;
import com.fintech.notification.websocket.TransactionWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final TransactionWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public NotificationController(TransactionWebSocketHandler webSocketHandler, ObjectMapper objectMapper) {
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Endpoint REST permettant aux autres microservices (transaction, risk) de diffuser un événement
     * sur le canal WebSocket /ws/tx.
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastEvent(@RequestBody NotificationEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now().toString());
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            webSocketHandler.broadcast(jsonPayload);
            return ResponseEntity.ok(Map.of(
                    "status", "BROADCASTED",
                    "clients_reached", webSocketHandler.getActiveSessionCount(),
                    "event_type", event.getType() != null ? event.getType() : "UNKNOWN"
            ));
        } catch (JsonProcessingException e) {
            log.error("Erreur de sérialisation JSON pour la notification : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "Impossible de sérialiser l'événement"
            ));
        }
    }

    /**
     * Statut de l'infrastructure WebSocket (nombre de clients connectés).
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "active_websocket_clients", webSocketHandler.getActiveSessionCount(),
                "channel", "/ws/tx",
                "timestamp", Instant.now().toString()
        ));
    }
}
