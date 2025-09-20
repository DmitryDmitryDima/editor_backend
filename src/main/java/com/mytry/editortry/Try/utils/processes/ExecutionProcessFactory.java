package com.mytry.editortry.Try.utils.processes;


import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExecutionProcessFactory {



    @Autowired
    private ProjectExecutionStateManager stateService;

    // база активных процессов. Через нее можно контролировать жизненный цик процессс. Ключ - айди проекта
    private final ConcurrentHashMap<Long, ExecutionProcess> processes = new ConcurrentHashMap<>();





    // создаем и запускаем процесс
    @Async("projectExecutor")
    public void createExecutionProcess(Long projectId){





        ExecutionProcess executionProcess = new ExecutionProcess(stateService, projectId);
        // заносим процесс в хранилище
        processes.put(projectId, executionProcess);


        executionProcess.start();
    }

    // ручная остановка процесса
    public void stopExecutionProcess(Long projectId){


        ExecutionProcess process = processes.get(projectId);
        if (process != null){
            process.stop();

        }

    }



    }







