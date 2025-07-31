package com.mytry.editortry.Try.utils.websocket.stomp.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// todo пока хз, нужна ли вообще эта информация
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileSaveInfo implements MetaInfo{
    private Long file_id;
    private Long project_id;
}
