package com.mytry.editortry.Try.utils.websocket.stomp.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessWebsocketEvent {
    private String message;
    private WebSocketEventType type;
}
