package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionAnswer;
import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionRequest;
import com.mytry.editortry.Try.dto.files.EditorFileReadAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileReadRequest;
import com.mytry.editortry.Try.dto.files.EditorFileSaveAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileSaveRequest;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.service.codeanalyzis.CodeAnalyzer;
import com.mytry.editortry.Try.utils.cache.CacheSystem;
import com.mytry.editortry.Try.utils.websocket.stomp.events.EventType;
import com.mytry.editortry.Try.utils.websocket.stomp.RealtimeEvent;
import com.mytry.editortry.Try.utils.websocket.stomp.events.FileSaveInfo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Service
public class EditorService {


    // рабочая директория на диске
    @Value("${files.directory}")
    private String disk_directory;

    // репозитории
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FileRepository fileRepository;

    // вебсокет уведомления
    @Autowired
    private SimpMessagingTemplate notifier;

    // cache system
    @Autowired
    private CacheSystem cacheSystem;



    @Autowired
    private CodeAnalyzer codeAnalyzer;





    public EditorBasicSuggestionAnswer basicSuggestion(EditorBasicSuggestionRequest request){

        // формирование внешней предложки - первоначально происходит обращение к кешу по project id
        // если он существует, значит он актуален
        // todo cache system manipulation



        return codeAnalyzer.basicSuggestion(request);
    }









    /*

    сохранение файла

    todo Операция с кешем - точечно обновляем кеш файла через id
    Уведомляем кеш о том, что проект был изменен

     */
    @Transactional(rollbackOn = Exception.class)
    public EditorFileSaveAnswer saveFile(EditorFileSaveRequest request){






        EditorFileSaveAnswer editorFileSaveAnswer = new EditorFileSaveAnswer();
        File file = fileRepository.findById(request.getFile_id())
                .orElseThrow(()->new IllegalArgumentException("invalid id"));


        // СРАВНИВАЕМ ФРОНТЕНД ВРЕМЯ И ВРЕМЯ ПОСЛЕДНЕГО ИЗМЕНЕНИЯ - ЕСЛИ ИЗМЕНЕНИЕ в базе БЫЛО ПОЗЖЕ, ТО ЭТО РАССИНХРОН!
        Instant databaseTime = file.getUpdatedAt();
        Instant clientTime = request.getClientTime();




        if (databaseTime.isAfter(clientTime)){
            throw new IllegalArgumentException("time synchronization error");
        }





        Project project = projectRepository.findById(request.getProject_id()).orElseThrow(()->
            new IllegalArgumentException("invalid project id")
        );


        String username = project.getOwner().getUsername();

        // вычисляем путь к файловой системе - проверка целостности выполняется только при fetch
        String disk_path = disk_directory+username+"/projects/"+project.getName()+"/"+request.getFull_path();

        try (FileWriter writer = new FileWriter(disk_path)) {
            writer.write(request.getContent());
        } catch (IOException e) {
            throw new IllegalArgumentException("file update fail");
        }




        // фиксируем время изменения файла для контроля изменений
        Instant time = Instant.now();
        file.setUpdatedAt(time);
        editorFileSaveAnswer.setUpdatedAt(time);

        // формируем событие изменения файла
        RealtimeEvent realtimeEvent = new RealtimeEvent();
        realtimeEvent.setType(EventType.FILE_SAVE);
        realtimeEvent.setTime(time);
        FileSaveInfo fileSaveInfo = new FileSaveInfo();
        fileSaveInfo.setProject_id(project.getId());
        fileSaveInfo.setFile_id(file.getId());
        realtimeEvent.setMetaInfo(fileSaveInfo);

        // клиентский одноразовый ключ для идентификации ивента
        realtimeEvent.setEvent_id(request.getEvent_id());

        // информация отправляется на два направления
        notifier.convertAndSend("/projects/"+project.getId()+"/"+file.getId(), realtimeEvent );
        notifier.convertAndSend("/projects/"+project.getId(), realtimeEvent);





        // уведомляем кеш систему о том, что в проекте произошли изменения
        cacheSystem.setProjectChange(project.getId());






        return editorFileSaveAnswer;
    }


    public EditorFileReadAnswer loadFile(EditorFileReadRequest request){

        // get props
        String username = request.getUsername();
        String projectname = request.getProjectname();
        String fullPath = request.getFullPath();


        Project project = projectRepository.findByOwnerUsernameAndName(username, projectname)
                .orElseThrow(()-> new IllegalArgumentException("no project found")
                );

        EditorFileReadAnswer editorFileReadAnswer = new EditorFileReadAnswer();
        editorFileReadAnswer.setProject_id(project.getId());

        /*
        извлекаем file_id, заодно проверяя целостность файловой системы

         */

        String[] path = fullPath.split("/");



        Directory directory = project.getRoot();

        File file = null;

        for (int x = 0; x<path.length; x++){
            String step = path[x];
            // сравниваем файлы
            if (x== path.length-1){
                List<File> files = directory.getFiles();
                file = files.stream().filter(el->(el.getName()+"."+el.getExtension())
                                .equals(step))
                        .findAny()
                        .orElseThrow(()->
                                new IllegalArgumentException("no file found")
                        );



            }

            else {
                List<Directory> children = directory.getChildren();
                String parent = directory.getName();
                //System.out.println("step "+step);
                //System.out.println(children);
                directory = children.stream().filter(el->el.getName().equals(step)).findAny().orElseThrow(()->
                        new IllegalArgumentException("on parent "+parent +" no directory with name "+step+ " inside "+children)
                );
            }
        }

        editorFileReadAnswer.setFile_id(file.getId());

        // загружаем файл из диска
        java.io.File disk_file = new java.io.File(disk_directory+"/"+username+"/projects/"+projectname+"/"+fullPath);
        StringBuilder sb = new StringBuilder();

        try (FileReader r = new FileReader(disk_file);
             BufferedReader bufferedReader = new BufferedReader(r);
        ){

            String s;
            while ((s = bufferedReader.readLine())!=null){
                
                sb.append(s+"\n");
            }
        }
        catch (Exception e){
            throw new IllegalArgumentException("error while reading disk");
        }



        editorFileReadAnswer.setContent(sb.toString());
        editorFileReadAnswer.setUpdatedAt(Instant.now());








        return editorFileReadAnswer;
    }
}
