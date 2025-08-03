package com.mytry.editortry.Try.utils.websocket.stomp;

import com.mytry.editortry.Try.utils.cache.CacheSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Arrays;

@Component
public class WebsocketLifecycle {

    @Autowired
    private CacheSystem cacheSystem;



    @EventListener
    private void handleSessionSubscribe(SessionSubscribeEvent event){
        System.out.println("subscribed");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();

        // for project
        if (destination!=null && destination.startsWith("/projects/")){

            String topic = destination.split("/projects/")[1];
            String[] topicDivided = topic.split("/");

            Long projectId = Long.valueOf(topicDivided[0]);
            cacheSystem.addProjectSubscription(headerAccessor.getSessionId(), projectId);

        }
    }



    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        System.out.println("disconnected");

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        cacheSystem.removeProjectSubscription(headerAccessor.getSessionId());

    }


}
