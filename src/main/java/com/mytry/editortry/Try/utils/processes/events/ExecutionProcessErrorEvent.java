package com.mytry.editortry.Try.utils.processes.events;

import org.springframework.context.ApplicationEvent;

public class ExecutionProcessErrorEvent extends ApplicationEvent {
        private String message;
        private Long projectId;

    public ExecutionProcessErrorEvent(Object source, String message, Long projectId) {
            super(source);
            this.message = message;
            this.projectId = projectId;
    }
}
