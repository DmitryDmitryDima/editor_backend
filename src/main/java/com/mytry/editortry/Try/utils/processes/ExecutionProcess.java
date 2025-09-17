package com.mytry.editortry.Try.utils.processes;

// процесс генерирует события в отдельном потоке и отправляет их на хост
public class ExecutionProcess implements Runnable{

    private ExecutionProcessFactory host;
    public ExecutionProcess(ExecutionProcessFactory factory){
        host = factory;
    }


    @Override
    public void run() {

    }
}
