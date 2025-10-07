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

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();

        System.out.println("subscribed with session id "+headerAccessor.getSessionId());

        // for project
        if (destination!=null && destination.startsWith("/projects/java/")){

            String topic = destination.split("/projects/java/")[1];
            String[] topicDivided = topic.split("/");

            Long projectId = Long.valueOf(topicDivided[0]);
            Long fileId = null;
            if (topicDivided.length==2){
                fileId = Long.valueOf(topicDivided[1]);
            }
            cacheSystem.addProjectSubscription(headerAccessor.getSessionId(), projectId);

        }
    }



    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {


        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println("unsubscribed with session id "+headerAccessor.getSessionId());
        cacheSystem.removeProjectSubscription(headerAccessor.getSessionId());

    }


}
