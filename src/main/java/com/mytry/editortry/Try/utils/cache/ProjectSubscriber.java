package com.mytry.editortry.Try.utils.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSubscriber {

    private String sessionId;

    // фиксируем, если просматривается конкретный файл. == null, если просматривается проект
    private Long file_id;

}
