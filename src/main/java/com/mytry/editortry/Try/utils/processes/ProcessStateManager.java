package com.mytry.editortry.Try.utils.processes;

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

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// бин, слушающий запросы на запуск
@Component
public class ProcessStateManager {

    @Autowired
    private ProjectDatabaseLock databaseLock;



    // точка для передачи вебсокет сообщений
    @Autowired
    private SimpMessagingTemplate notifier;

    @Autowired
    private ProjectLogger projectLogger;

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

        // логируем в файл
        projectLogger.addToLog(messageEvent.getMessage(), messageEvent.getProjectId(), messageEvent.getDirectory());

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





             try {
                 databaseLock.unlock(interruptionEvent.getProjectId());
             }
             catch (Exception e){
                 // внутренняя ошибка
             }

             // логируем
             projectLogger.addToLog("process stopped at "+Instant.now(), interruptionEvent.getProjectId(),
                     interruptionEvent.getDirectory());

             projectLogger.clearLockObject(interruptionEvent.getProjectId());




         // событие пришло извне - мы должны найти процесс и послать ему сигнал об остановке, после чего он уже сформируем свое событие
         } else {

             try {
                 databaseLock.checkIfStopped(interruptionEvent.getProjectId());
             }
             catch (Exception e){
                 ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent(e.getMessage(),
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



    @EventListener
    @Async("projectExecutor")
    public void createProcess(ExecutionProcessCreationEvent event){
        System.out.println("creation event start");

        ExecutionProcessWithCallback preparedProcess = event.getProcess();

        String path;

        try {
            path = databaseLock.lockProjectAndGenerateDiskPath(preparedProcess.getProjectId());
        }
        catch (Exception e){
            ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent(e.getMessage(),
                    WebSocketEventType.PROCESS_INIT_ERROR);
            notifier.convertAndSend("/projects/"+preparedProcess.getProjectId(),websocketEvent);

            return;
        }



        ProcessWebsocketEvent websocketEvent = new ProcessWebsocketEvent("project execution initialization...",
                WebSocketEventType.PROCESS_INIT);
        notifier.convertAndSend("/projects/"+preparedProcess.getProjectId(), websocketEvent);

        // подготавливаем процесс к запуску
        preparedProcess.setProjectDirectory(disk_address+path);


        // заносим в хранилище процессов // todo баг кейс - процесс уже существует
        processes.put(preparedProcess.getProjectId(), preparedProcess);

        // логируем запись о старте
        projectLogger.clearLog(preparedProcess.getProjectId(), disk_address+path);
        projectLogger.addToLog("project start at "+ Instant.now(),
                preparedProcess.getProjectId(), disk_address+path);



        preparedProcess.start();







    }







}
