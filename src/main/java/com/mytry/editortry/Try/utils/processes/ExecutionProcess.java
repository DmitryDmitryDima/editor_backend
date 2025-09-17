package com.mytry.editortry.Try.utils.processes;

import org.springframework.scheduling.annotation.Async;

// процесс генерирует события в отдельном потоке и отправляет их на хост
// необходима реализация прерывания
public class ExecutionProcess {

    private ExecutionProcessFactory host;
    public ExecutionProcess(ExecutionProcessFactory factory){
        host = factory;
    }

    @Async("projectExecutor")
    public void start(){

    }



}

