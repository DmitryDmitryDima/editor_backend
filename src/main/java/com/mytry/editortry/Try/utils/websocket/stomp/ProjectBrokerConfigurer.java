package com.mytry.editortry.Try.utils.websocket.stomp;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ProjectBrokerConfigurer implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/projects"); // сюда клиент подписывается {projectId}/{file_id}
        registry.setApplicationDestinationPrefixes("/realtime"); // на адрес с этим префиксом клиент отправляет ивенты
        // (ловим в спец контроллере)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/project").setAllowedOriginPatterns("*");//  сюда мы подключаемся


    }


}
