package com.mytry.editortry.Try.utils.processes.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class ExecutionProcessErrorEvent extends ApplicationEvent {
        private String message;
        private Long projectId;
        private String directory;

    public ExecutionProcessErrorEvent(Object source, String message, Long projectId, String directory) {
            super(source);
            this.message = message;
            this.projectId = projectId;
    }
}
