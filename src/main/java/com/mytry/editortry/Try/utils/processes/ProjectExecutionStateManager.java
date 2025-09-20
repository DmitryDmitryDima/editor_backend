package com.mytry.editortry.Try.utils.processes;

import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessEvent;
import com.mytry.editortry.Try.utils.processes.events.ProcessEventType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProjectExecutionStateManager {

    @Autowired
    private ProjectRepository projectRepository;

    // точка для передачи вебсокет сообщений
    @Autowired
    private SimpMessagingTemplate notifier;

    @Value("${files.directory}")
    private String disk_address;


    // запрос на старт - проверяем все условия
    public String onStart(Long id){
        // проверяем, существует ли проект
        Project project = projectRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("no such project exists"));
        // проверяем, существует ли точка входа в проект
        File entryPoint = project.getEntryPoint();
        if (entryPoint==null){
            throw new IllegalStateException("no entry point");
        }



        if (project.isRunning()){
            throw new IllegalStateException("project is already running");
        }

        // блокируем проект, создаем процесс, последовательно выполняющий необходимые действия и отправляющий уведомления
        project.setRunning(true);

        // формируем адрес директории
        return disk_address+project.getOwner().getUsername()+"/projects/"+project.getName()+"/";
    }

    public void onEvent(ExecutionProcessEvent event){
        // если пришло сообщение о конце процесса, мы снимаем блок в бд и удаляем процесс из хранилища
        if (event.getType().equals(ProcessEventType.PROCESS_END)){
            destroyProcess(event);
        }

        // уведомляем всех подписчиков проекта

        String address = "/projects/"+event.getProjectId();
        notifier.convertAndSend(address, event);
    }

    private void destroyProcess(ExecutionProcessEvent event) {
        Project project = projectRepository
                .findById(event.getProjectId()).orElseThrow(()->new IllegalArgumentException("no such project exists"));
        project.setRunning(false);

    }


}
