package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.basicsuggestion.*;
import com.mytry.editortry.Try.dto.dotsuggestion.EditorDotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.EditorDotSuggestionRequest;
import com.mytry.editortry.Try.dto.files.EditorFileReadAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileReadRequest;
import com.mytry.editortry.Try.dto.files.EditorFileSaveAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileSaveRequest;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.service.codeanalyzis.CodeAnalyzer;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectFile;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class EditorServiceOld {



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



    /*
    во многом опора идет на анализ импортируемых данных - с их помощью мы можем узнать,
    какие типы нужно просканировать в кеше/зависимостях
    в данном методе так же, при необходимости, нужно пересобирать кеш
     */
    public EditorDotSuggestionAnswer dotSuggestion(EditorDotSuggestionRequest request){

        // шаг 1 - Проверка существования кеша проекта
        Map<String, List<CacheSuggestionInnerProjectFile>> cache = cacheSystem.getProjectCacheState(request.getProject_id());


        // пересборка кеша
        if (cache == null){
            System.out.println("Формируем кеш проекта");
            Project project = projectRepository
                    .findById(request.getProject_id()).orElseThrow(()-> new IllegalArgumentException("project not found"));
            User owner = project.getOwner();
            String projectname = project.getName();

            List<String> mavenStructure = List.of("src", "main","java","com");

            // com directory
            Directory current = project.getRoot();
            for (String s:mavenStructure){
                Optional<Directory> candidate = current.getChildren().stream().filter(el->el.getName().equals(s)).findAny();
                if (candidate.isEmpty()) throw new IllegalArgumentException("invalid project structure");
                current = candidate.get();

            }

            // формируем путь, следуя классической maven структуре
            String path = disk_directory+ owner.getUsername()+"/projects/"+projectname+"/src/main/java";

            ProjectTypesDTO projectTypesDTO = codeAnalyzer.analyzeProject(current, path);
            cache = projectTypesDTO.getPackageToFileAssociation();
            // обновляем кеш
            cacheSystem.updateProjectCache(request.getProject_id(),
                    projectTypesDTO.getPackageToFileAssociation(),
                    projectTypesDTO.getIdToFileAssociation() );
        }


        return codeAnalyzer.dotSuggestion(request, cache);
    }







    /*
    логика извлечения из кеша - если кеш проекта пустой (ассоциации = null), то мы должны его пересобрать
     */
    public EditorBasicSuggestionAnswer basicSuggestion(EditorBasicSuggestionRequest request){

        //System.out.println(request);

        EditorBasicSuggestionAnswer editorBasicSuggestionAnswer = new EditorBasicSuggestionAnswer();


        /*
        шаг 1 - Формирование context based предложки
         */

        BasicSuggestionContextBasedInfo contextBasedInfo = codeAnalyzer.basicSuggestionContextBasedAnalysis(request);

        editorBasicSuggestionAnswer.setContextBasedInfo(contextBasedInfo);

        /*
        шаг 2 - Формирование внешней предложки

        тут мы должны вытянуть/сформировать кеш, отфильтровать его по "доступности" и введенному символу

         */

        // Проверяем, существует ли кеш
        Map<String, List<CacheSuggestionInnerProjectFile>> cache = cacheSystem.getProjectCacheState(request.getProject_id());


        // пересборка кеша
        if (cache == null){
            System.out.println("Формируем кеш проекта");
            Project project = projectRepository
                    .findById(request.getProject_id()).orElseThrow(()-> new IllegalArgumentException("project not found"));
            User owner = project.getOwner();
            String projectname = project.getName();

            List<String> mavenStructure = List.of("src", "main","java","com");

            // com directory
            Directory current = project.getRoot();
            for (String s:mavenStructure){
                Optional<Directory> candidate = current.getChildren().stream().filter(el->el.getName().equals(s)).findAny();
                if (candidate.isEmpty()) throw new IllegalArgumentException("invalid project structure");
                current = candidate.get();

            }

            // формируем путь, следуя классической maven структуре
            String path = disk_directory+ owner.getUsername()+"/projects/"+projectname+"/src/main/java";

            ProjectTypesDTO projectTypesDTO = codeAnalyzer.analyzeProject(current, path);
            cache = projectTypesDTO.getPackageToFileAssociation();
            // обновляем кеш
            cacheSystem.updateProjectCache(request.getProject_id(),
                    projectTypesDTO.getPackageToFileAssociation(),
                    projectTypesDTO.getIdToFileAssociation() );
        }

        // todo анализ кеша и формирование ответа



        List<BasicSuggestionType> types = new ArrayList<>();

        for (Map.Entry<String, List<CacheSuggestionInnerProjectFile>> entry:cache.entrySet()){

            List<CacheSuggestionInnerProjectFile> files = entry.getValue();


            for (var f:files){
                if (f.getPublicType().getName().startsWith(request.getText())){
                    BasicSuggestionType basicSuggestionType = new BasicSuggestionType();
                    // формируем импорт только в случае, если не совпадает package
                    if (!contextBasedInfo.getPackageWay().equals(f.getPackageWay())){
                        basicSuggestionType.setPackageWay(f.getPackageWay());
                    }
                    //basicSuggestionProjectType.setPackageWay(f.getPackageWay());
                    basicSuggestionType.setName(f.getPublicType().getName());
                    types.add(basicSuggestionType);
                }
            }
        }
        editorBasicSuggestionAnswer.setProjectTypes(types);

        // формируем предложку из внешних библиотек - пока что только java. данная предложка доступна только для позици внутри типа
        if (!contextBasedInfo.isOutsideOfType()){

            List<BasicSuggestionType> outer = new ArrayList<>();
            var javaLibrarySuggestion = cacheSystem.getStandartLibraryTypesByFragment(request.getText());
            javaLibrarySuggestion.forEach(el->{
                BasicSuggestionType basicSuggestionType = new BasicSuggestionType();
                basicSuggestionType.setName(el.getName());
                basicSuggestionType.setPackageWay(el.getPackageWay());
                outer.add(basicSuggestionType);
            });

            editorBasicSuggestionAnswer.setOuterTypes(outer);
        }








        return editorBasicSuggestionAnswer;
    }









    /*

    сохранение файла

     ! Операции с кешем
     - точечно обновляем кеш файла через id
     - Уведомляем кеш о том, что проект был изменен

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

        // точечно обновляем файловый кеш
        try {
            cacheSystem.updateFileCache(project.getId(),
                    file.getId(),
                    codeAnalyzer.generateFileCache(request.getContent()));
        }
        catch (Exception e){
            e.printStackTrace();
        }






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
