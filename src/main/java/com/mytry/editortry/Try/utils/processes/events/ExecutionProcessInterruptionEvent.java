package com.mytry.editortry.Try.utils.processes.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class ExecutionProcessInterruptionEvent extends ApplicationEvent {
    private Long projectId;
    private InterruptionType interruptionType;


    public ExecutionProcessInterruptionEvent(Object source, Long projectId, InterruptionType interruptionType) {
        super(source);
        this.projectId = projectId;
        this.interruptionType = interruptionType;
    }

    public static enum InterruptionType{External, Internal}
}
