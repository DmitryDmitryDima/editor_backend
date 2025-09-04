package com.mytry.editortry.Try.service;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.mytry.editortry.Try.dto.files.EditorFileReadAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileReadRequest;
import com.mytry.editortry.Try.dto.files.EditorFileSaveAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileSaveRequest;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectFile;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectType;
import com.mytry.editortry.Try.utils.cache.CacheSystem;
import com.mytry.editortry.Try.utils.cache.ProjectCache;
import com.mytry.editortry.Try.utils.websocket.stomp.RealtimeEvent;
import com.mytry.editortry.Try.utils.websocket.stomp.events.EventType;
import com.mytry.editortry.Try.utils.websocket.stomp.events.FileSaveInfo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
public class EditorService {


    // рабочая директория на диске - проекты
    @Value("${files.directory}")
    private String disk_directory;

    // кеш приложения - отсюда мы берем кеш проекта и внешних зависимостей
    @Autowired
    private CacheSystem cacheSystem;


    // репозитории

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private FileRepository fileRepository;


    // точка для передачи вебсокет сообщений
    @Autowired
    private SimpMessagingTemplate notifier;

    /*
    заранее конфигурированный java parser для анализа кода
     */
    @Autowired
    private JavaParser parser;

    /*методы класса object*/
    private final List<String> objectMethods = List
            .of("wait", "getClass", "hashCode","toString","clone", "equals", "notify", "notifyAll");





    /*
    Сохранение текущего состояния файла
    Запрос будет защищен при введении security, поэтому он опирается на посылаемые id проекта и файла
    Операция сохранения взаимодействует с диском (запись), кешем (обновление file api) и вебсокетом
    Ивент посылается всем подписчикам проекта, заставляя их обновить содержимое их окон (в зависимости от их места в проекте)
     */

    @Transactional(rollbackOn = Exception.class)
    public EditorFileSaveAnswer saveFile(EditorFileSaveRequest request){
        EditorFileSaveAnswer answer = new EditorFileSaveAnswer();

        // загружаем сущность файла, проверяем. существует ли она
        File file = fileRepository.findById(request.getFile_id())
                .orElseThrow(()->new IllegalArgumentException("invalid id"));

        /* проверка на рассинхрон - если время последнего сохранения в базе позже,
         чем время запроса на сохранение - пользователь пытается сохранить неактуальный текст
         */
        Instant databaseTime = file.getUpdatedAt();
        Instant clientTime = request.getClientTime();
        if (databaseTime.isAfter(clientTime)){
            throw new IllegalArgumentException("time synchronization error");
        }

        // конструируем путь к файлу на диске
        Project project = projectRepository.findById(request.getProject_id()).orElseThrow(()->
                new IllegalArgumentException("invalid project id")
        );

        String username = project.getOwner().getUsername();
        String disk_path = disk_directory+username+"/projects/"+project.getName()+"/"+request.getFull_path();

        // записываем информацию на диск
        try (FileWriter writer = new FileWriter(disk_path)) {
            writer.write(request.getContent());
        } catch (IOException e) {
            throw new IllegalArgumentException("file update fail");
        }

        // фиксируем время записи в базе данных и в ответе сервера
        Instant time = Instant.now();
        file.setUpdatedAt(time);
        answer.setUpdatedAt(time);

        /*
        формируем и посылаем событие для вебсокет подписчиков
        из запроса берем авторский ключ, который записываем в ивент.
        Так автор события не будет реагировать на свое же действие
        todo данный функционал может изменится с введением авторизации через токены
         */
        RealtimeEvent realtimeEvent = new RealtimeEvent();
        realtimeEvent.setType(EventType.FILE_SAVE);
        realtimeEvent.setTime(time);
        FileSaveInfo fileSaveInfo = new FileSaveInfo();
        fileSaveInfo.setProject_id(project.getId());
        fileSaveInfo.setFile_id(file.getId());
        realtimeEvent.setMetaInfo(fileSaveInfo);


        realtimeEvent.setEvent_id(request.getEvent_id()); // пробрасываем ключ

        // информация отправляется на два направления - для подписчиков главной страницы проекта, а также файлов
        notifier.convertAndSend("/projects/"+project.getId()+"/"+file.getId(), realtimeEvent );
        notifier.convertAndSend("/projects/"+project.getId(), realtimeEvent);


        /*
        операции с кешем
         - пересобираем api файла (внутренний для проекта) с помощью java parser
         - вносим его в кеш, передавая время обновления
         */

        ProjectCache projectCache;
        try{
            projectCache = cacheSystem.getProjectCache(project.getId());
        }
        catch (IllegalStateException e){
            // в случае ошибки взаимодействия с кешем (кеш равен null) все равно отправляем ответ
            return answer;
        }

        // если кеш не пустой, конструируем файловый api при помощи java parser, тем самым точечно обновляя его
        if (!projectCache.isEmpty()){
            try {
                CacheSuggestionInnerProjectFile fileAPI = generateAPIforInnerFile(request.getContent());
                projectCache.updateFileCache(file.getId(), fileAPI, time);

            }
            // файл может быть написан с ошибками, что вызовет ошибку при парсинге
            catch (Exception e){
                return answer;
            }

        }



        return answer;



    }


    /*
    Загрузка содержимого файла
    Данный метод опирается на username, projectname и fullpath к файлу,
     так как операция чтения по желанию пользователя может быть доступна всем
     */

    public EditorFileReadAnswer readFile(EditorFileReadRequest request){

        // для удобства извлекаем данные, которыми будем оперировать
        String username = request.getUsername();
        String projectname = request.getProjectname();
        String fullPath = request.getFullPath();


        EditorFileReadAnswer editorFileReadAnswer = new EditorFileReadAnswer();

        // загружаем проект (вся структура одним запросом), проверяя его наличие в базе данных
        Project project = projectRepository.findByOwnerUsernameAndNameWithStructure(username, projectname)
                .orElseThrow(()-> new IllegalArgumentException("project not found")
                );

        /*
        суть дальнейшего алгоритма - мы сравниваем полученный путь со структурой проекта в базе данных (начиная с root)
        Наша цель - добраться до сущности file
         */




        // превращаем путь в массив для его дальнейшей валидации
        String[] path = fullPath.split("/");

        // переменная directory для прохода вглубь структуры проекта - начинаем с root папки
        Directory directory = project.getRoot();

        // готовим переменную file для искомой сущности
        File file = null;

        // итерируемся по элементам полученного пути к файлу
        for (int x = 0; x<path.length; x++){
            String step = path[x];
            // если мы достигли последнего элемента - это имя файла, запрашиваем список файлов
            if (x== path.length-1){
                List<File> files = directory.getFiles();
                file = files.stream().filter(el->(el.getName()+"."+el.getExtension())
                                .equals(step))
                        .findAny()
                        .orElseThrow(()->
                                new IllegalArgumentException("no file with name "+step)
                        );



            }
            // если элемент не является последним, мы проверяем все директории
            else {
                List<Directory> children = directory.getChildren();
                String parent = directory.getName();
                directory = children.stream().filter(el->el.getName().equals(step)).findAny().orElseThrow(()->
                        new IllegalArgumentException("on parent "+parent +" no directory with name "+step+ " inside "+children)
                );
            }
        }

        // проверка пути завершена - загружаем содержимое файла из диска
        String disk_path = disk_directory+"/"+username+"/projects/"+projectname+"/"+fullPath;
        String content;

        try {
            content = Files.readString(Path.of(disk_path), StandardCharsets.UTF_8);
        }
        catch (IOException ioException){
            throw new RuntimeException("fiel read error");
        }

        // формируем ответ
        editorFileReadAnswer.setFile_id(file.getId());
        editorFileReadAnswer.setProject_id(project.getId());
        editorFileReadAnswer.setUpdatedAt(Instant.now());
        editorFileReadAnswer.setContent(content);

        return editorFileReadAnswer;


    }




    // генерируем api для внутреннего файла проекта - public и default компоненты
    private CacheSuggestionInnerProjectFile generateAPIforInnerFile(String code){
        CacheSuggestionInnerProjectFile file = new CacheSuggestionInnerProjectFile();
        CompilationUnit astTree = parser.parse(code).getResult().orElseThrow(()->new IllegalArgumentException("parsing failed"));

        String packageDeclaration = (astTree.getPackageDeclaration().orElseThrow(()-> new IllegalArgumentException("no package")))
                .getNameAsString();
        file.setPackageWay(packageDeclaration);

        astTree.getTypes().forEach(type->{

            CacheSuggestionInnerProjectType cacheType = new CacheSuggestionInnerProjectType();
            cacheType.setName(type.getNameAsString());
            // определяем access modifier для типа
            if (type.isPublic()){
                file.setPublicType(cacheType);
            }
            else if (!type.isPrivate() && !type.isNestedType() && !type.isProtected()){
                file.getDefaultTypes().add(cacheType);
            }
            // анализируем методы - разделяем статичные и нестатичные методы для удобства чтения кеша
            type.getMethods().forEach(method->{
                String name = method.getNameAsString();
                if (method.isStatic()){
                    if (method.isPublic()){
                        cacheType.getPublicStaticMethods().add(name);
                    }
                    else if (method.isDefault()){
                        cacheType.getDefaultStaticMethods().add(name);
                    }
                }
                else {
                    if (method.isPublic()){
                        cacheType.getPublicMethods().add(name);
                    }
                    else if (method.isDefault()){
                        cacheType.getDefaultMethods().add(name);
                    }
                }

            });

            // анализируем поля - разделяем статичные и нестатичные поля для удобства чтения кеша
            type.getFields().forEach(fieldDeclaration -> {

                String fieldName = fieldDeclaration.findAll(VariableDeclarator.class).getFirst().getNameAsString();

                if (fieldDeclaration.isStatic()){
                    if (fieldDeclaration.isPublic()){
                        cacheType.getPublicStaticFields().add(fieldName);

                    }
                    else if (!fieldDeclaration.isPrivate() && !fieldDeclaration.isProtected()){
                        cacheType.getDefaultStaticFields().add(fieldName);
                    }
                }
                else {
                    if (fieldDeclaration.isPublic()){
                        cacheType.getPublicFields().add(fieldName);

                    }
                    else if (!fieldDeclaration.isPrivate() && !fieldDeclaration.isProtected()){
                        cacheType.getDefaultFields().add(fieldName);
                    }
                }


            });



        });


        return file;
    }






}
