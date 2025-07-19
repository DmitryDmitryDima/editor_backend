package com.mytry.editortry.Try.utils.websocket;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;

import java.io.IOException;

@Component
public class WebSocketLogger {

    @Autowired
    private WebsocketMessageHandler websocketMessageHandler;


    public void log(String message){
        websocketMessageHandler.getWebSocketSessions().forEach(session->{
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
