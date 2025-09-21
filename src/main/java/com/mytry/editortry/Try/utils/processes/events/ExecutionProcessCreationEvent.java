package com.mytry.editortry.Try.utils.processes.events;


import com.mytry.editortry.Try.utils.processes.ExecutionProcessWithCallback;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class ExecutionProcessCreationEvent extends ApplicationEvent {

    private ExecutionProcessWithCallback process;
    public ExecutionProcessCreationEvent(Object source, ExecutionProcessWithCallback preparedProcess) {
        super(source);

        this.process = preparedProcess;
    }


}
