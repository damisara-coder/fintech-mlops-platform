package com.fintech.notification.config;

import com.fintech.notification.websocket.TransactionWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuration Spring WebSocket enregistrant le endpoint /ws/tx.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TransactionWebSocketHandler webSocketHandler;

    public WebSocketConfig(TransactionWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Enregistrement du endpoint WebSocket pur /ws/tx avec CORS permissif pour le Frontend React
        registry.addHandler(webSocketHandler, "/ws/tx")
                .setAllowedOrigins("*");

        // Enregistrement avec fallback SockJS pour les navigateurs ou environnements proxy restreints
        registry.addHandler(webSocketHandler, "/ws/tx/sockjs")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
