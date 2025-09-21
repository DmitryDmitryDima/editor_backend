package com.mytry.editortry.Try.utils.processes;

import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessCreationEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessInterruptionEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessMessageEvent;
import com.mytry.editortry.Try.utils.websocket.stomp.events.ProcessWebsocketEvent;
import com.mytry.editortry.Try.utils.websocket.stomp.events.WebSocketEventType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// бин, слушающий запросы на запуск
@Component
public class ProcessStateManager {

    @Autowired
    private ProjectRepository projectRepository;

    // точка для передачи вебсокет сообщений
    @Autowired
    private SimpMessagingTemplate notifier;

    @Value("${files.directory}")
    private String disk_address;


    // база активных процессов. Через нее можно контролировать жизненный цик процессс. Ключ - айди проекта
    private final ConcurrentHashMap<Long, ExecutionProcessWithCallback> processes = new ConcurrentHashMap<>();

    // todo логирование в файл
    @EventListener
    public void processMessage(ExecutionProcessMessageEvent messageEvent){
        ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent(messageEvent.getMessage(),
                WebSocketEventType.PROCESS_MESSAGE);
        notifier.convertAndSend("/projects/"+messageEvent.getProjectId(), websocketEvent);
    }


    @Transactional
    @EventListener
    public void processInterruption(ExecutionProcessInterruptionEvent interruptionEvent){

        // событие приходит изнутри процесса - он уже остановлен
         if (interruptionEvent.getInterruptionType().equals(ExecutionProcessInterruptionEvent.InterruptionType.Internal)){

             // очищаем процесс
             processes.remove(interruptionEvent.getProjectId());

             ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent("project terminated",
                     WebSocketEventType.PROCESS_END);

             notifier.convertAndSend("/projects/"+interruptionEvent.getProjectId(), websocketEvent);


             Project project = projectRepository.findById(interruptionEvent.getProjectId()).orElseThrow(()->
                 new IllegalStateException("process event trying to access non existent project")
             );

             // меняем флаг
             project.setRunning(false);




         // событие пришло извне - мы должны найти процесс и послать ему сигнал об остановке
         } else {
             Optional<Project> projectCheck = projectRepository.findById(interruptionEvent.getProjectId());
             if (projectCheck.isEmpty()){
                 ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent("no such project exists",
                         WebSocketEventType.PROCESS_INIT_ERROR);
                 notifier.convertAndSend("/projects/"+interruptionEvent.getProjectId(), websocketEvent);

                 return;
             }

             Project project = projectCheck.get();
             if (!project.isRunning()){
                 ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent("project is already stopped",
                         WebSocketEventType.PROCESS_STOP_ERROR);
                 notifier.convertAndSend("/projects/"+interruptionEvent.getProjectId(), websocketEvent);
                 return;
             }

             // если все хорошо - останавливаем процесс, после чего он сам сгенерирует событие остановки

            ExecutionProcessWithCallback process = processes.get(interruptionEvent.getProjectId());
            if (process!=null){
                process.stop();
            }
         }


    }


    @Transactional
    @EventListener
    @Async("projectExecutor")
    public void createProcess(ExecutionProcessCreationEvent event){
        System.out.println("creation event start");

        ExecutionProcessWithCallback preparedProcess = event.getProcess();
        Optional<Project> projectCheck = projectRepository.findById(preparedProcess.getProjectId());
        if (projectCheck.isEmpty()){
            ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent("no such project exists",
                    WebSocketEventType.PROCESS_INIT_ERROR);
            notifier.convertAndSend("/projects/"+preparedProcess.getProjectId(),websocketEvent);

            return;
        }

        Project project = projectCheck.get();




        // проверяем, существует ли точка входа в проект
        File entryPoint = project.getEntryPoint();
        if (entryPoint==null){
            ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent("no entry point",
                    WebSocketEventType.PROCESS_INIT_ERROR);
            notifier.convertAndSend("/projects/"+preparedProcess.getProjectId(),websocketEvent);
            return;
        }





        if (project.isRunning()){
            ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent("project is already running",
                    WebSocketEventType.PROCESS_INIT_ERROR);
            notifier.convertAndSend("/projects/"+preparedProcess.getProjectId(),websocketEvent);
            return;
        }



        // блокируем проект, создаем процесс, последовательно выполняющий необходимые действия и отправляющий уведомления
        project.setRunning(true);

        ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent("project execution initialization...",
                WebSocketEventType.PROCESS_INIT);
        notifier.convertAndSend("/projects/"+preparedProcess.getProjectId(), websocketEvent);

        // подготавливаем процесс к запуску
        preparedProcess.setProjectDirectory(disk_address+project.getOwner().getUsername()+"/projects/"+project.getName()+"/");

        // заносим в хранилище процессов // todo баг кейс - процесс уже существует
        processes.put(preparedProcess.getProjectId(), preparedProcess);

        preparedProcess.start();



    }







}
