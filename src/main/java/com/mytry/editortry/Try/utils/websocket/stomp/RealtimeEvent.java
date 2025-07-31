package com.mytry.editortry.Try.utils.websocket.stomp;

import com.mytry.editortry.Try.utils.websocket.stomp.events.EventType;
import com.mytry.editortry.Try.utils.websocket.stomp.events.MetaInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEvent{

    // тип ивента - базируясь на нем фронтенд будет думать, что делать
    private EventType type;


    // время события - критично важно для синхронизации параллельных действий
    private Instant time;

    // если есть, дополнительная информация по ивенту (к примеру, если есть какой то текстовый контент)
    private MetaInfo metaInfo;

}
