package com.mytry.editortry.Try.utils.processes.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionProcessEvent {
    private ProcessEventType type;

    private String message;

    private Long projectId;
}
