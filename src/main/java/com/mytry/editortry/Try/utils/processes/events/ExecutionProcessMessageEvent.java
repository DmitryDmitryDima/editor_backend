package com.mytry.editortry.Try.utils.processes.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class ExecutionProcessMessageEvent extends ApplicationEvent {

    private String message;
    private Long projectId;

    public ExecutionProcessMessageEvent(Object source, String message, Long projectId) {
        super(source);
        this.message = message;
        this.projectId = projectId;
    }
}
