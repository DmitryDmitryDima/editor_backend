package com.mytry.editortry.Try.utils.processes;


import com.mytry.editortry.Try.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExecutionProcessFactory {

    @Autowired
    private ProjectRepository projectRepository;

    // точка для передачи вебсокет сообщений
    @Autowired
    private SimpMessagingTemplate notifier;

    // база активных процессов. Через нее можно контролировать жизненный цик процессс. Ключ - айди проекта
    private ConcurrentHashMap<Long, ExecutionProcess> processes = new ConcurrentHashMap<>();


    // метод для уведомления хоста о том, что внутри процесса произошел некий event, который требуется послать адресату
    public void notifyHost(){

    }

}



