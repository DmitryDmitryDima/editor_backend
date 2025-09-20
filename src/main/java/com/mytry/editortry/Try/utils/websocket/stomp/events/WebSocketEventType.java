package com.mytry.editortry.Try.utils.websocket.stomp.events;

// события, о которых нужно уведомить клиентов. Если кто сделал save, то об этом должны знать все клиенты
public enum WebSocketEventType {
    FILE_SAVE, FILE_REMOVE, FILE_TRANSFER
}
