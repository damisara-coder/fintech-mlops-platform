package com.fintech.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.notification.controller.NotificationController;
import com.fintech.notification.dto.NotificationEvent;
import com.fintech.notification.websocket.TransactionWebSocketHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Test
    @DisplayName("Devrait diffuser un événement TX_DECIDED aux sessions WebSocket actives")
    void testBroadcastEvent() throws Exception {
        TransactionWebSocketHandler handler = new TransactionWebSocketHandler();
        WebSocketSession sessionMock = mock(WebSocketSession.class);
        when(sessionMock.isOpen()).thenReturn(true);
        when(sessionMock.getId()).thenReturn("ws-sess-1");

        handler.afterConnectionEstablished(sessionMock);
        assertEquals(1, handler.getActiveSessionCount());

        ObjectMapper mapper = new ObjectMapper();
        NotificationController controller = new NotificationController(handler, mapper);

        NotificationEvent event = NotificationEvent.builder()
                .type("TX_DECIDED")
                .txId("TX-999")
                .clientName("Laila Benali")
                .amount("250.00 EUR")
                .decision("APPROVED")
                .riskScore(12)
                .probability(0.12)
                .uncertaintyScore(0.05)
                .isUncertain(false)
                .build();

        ResponseEntity<Map<String, Object>> response = controller.broadcastEvent(event);

        assertNotNull(response);
        assertEquals("BROADCASTED", response.getBody().get("status"));
        assertEquals(1, response.getBody().get("clients_reached"));
        verify(sessionMock, times(1)).sendMessage(any(TextMessage.class));
    }
}
