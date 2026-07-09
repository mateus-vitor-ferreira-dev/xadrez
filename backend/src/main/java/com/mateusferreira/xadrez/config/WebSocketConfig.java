package com.mateusferreira.xadrez.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configura o WebSocket com STOMP (mensageria em tempo real).
 *
 * Fluxo:
 *  - Os clientes conectam no endpoint "/ws".
 *  - Eles SE INSCREVEM em tópicos do tipo "/topic/partidas/{id}".
 *  - Quando alguém joga, o servidor PUBLICA o novo estado nesse tópico, e o
 *    "broker" (corretor de mensagens) entrega a todos os inscritos na hora.
 *
 * @EnableWebSocketMessageBroker liga toda essa infraestrutura.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Reaproveita as mesmas origens do CORS (localhost no dev; *.vercel.app em prod).
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String[] origensPermitidas;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint que o front usa para abrir a conexão WebSocket (ws:// ou wss://).
        registry.addEndpoint("/ws").setAllowedOriginPatterns(origensPermitidas);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker simples, em memória: entrega mensagens publicadas em "/topic/...".
        registry.enableSimpleBroker("/topic");
        // Prefixo para mensagens vindas do cliente para o servidor (não usamos por ora).
        registry.setApplicationDestinationPrefixes("/app");
    }
}
