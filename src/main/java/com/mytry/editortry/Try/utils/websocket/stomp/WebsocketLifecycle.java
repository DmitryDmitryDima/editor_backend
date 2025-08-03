package com.mytry.editortry.Try.utils.websocket.stomp;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class WebsocketLifecycle {

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {



        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println(headerAccessor);
        System.out.println("connected");
        System.out.println(event.getSource());
        Message<byte[]> message = event.getMessage();
        String simpDestination = (String) message.getHeaders().get("simpDestination");
        System.out.println(simpDestination);
    }

    @EventListener
    private void handleSessionSubscribe(SessionSubscribeEvent event){
        System.out.println("subscribed");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println(headerAccessor);
    }



    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        System.out.println("disconnected");

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println(headerAccessor);
    }


}
