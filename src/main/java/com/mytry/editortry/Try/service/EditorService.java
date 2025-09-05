package com.mytry.editortry.Try.service;


import com.github.javaparser.JavaParser;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.mytry.editortry.Try.dto.basicsuggestion.BasicSuggestionContextBasedInfo;
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
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectFile;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectType;
import com.mytry.editortry.Try.utils.cache.CacheSystem;
import com.mytry.editortry.Try.utils.cache.ProjectCache;
import com.mytry.editortry.Try.utils.parser.CodeAnalysisUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

     /*ключевые слова, доступные в методе*/
     private final List<String> availableKeywordsForMethodAndConstructor = List.of("while","enum", "record","this", "try", "throw", "if", "int",
             "interface", "short", "super", "switch", "synchronized","do", "double", "final", "for", "float",
             "long", "class", "char","var","boolean", "byte","new"
     );



    /*
    формирование подсказки при вводе некоторого текстового фрагмента пользователем
    в данном методе - точка формирования кеша в случае его отсутствия

    todo так как тут по сути происходит постоянный анализ кода, о некоторых найденных ошибках пользователя можно уведомлять
     */
    public EditorBasicSuggestionAnswer basicSuggestion(EditorBasicSuggestionRequest request){

        EditorBasicSuggestionAnswer answer = new EditorBasicSuggestionAnswer();

        /*
        Шаг 1 - Сбор контексто-ориентированной информации, учитывающей текущий код и позицию в нем
         */
        try {
            BasicSuggestionContextBasedInfo context = contextBasedAnalysis(request);
            answer.setContextBasedInfo(context);
        }
        catch (Exception e){
            throw new RuntimeException("invalid code context");
        }





        return answer;

    }





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
        Project project = projectRepository.findByOwnerUsernameAndName(username, projectname)
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


                List<String> variables = fieldDeclaration.getVariables().stream().map(NodeWithSimpleName::getNameAsString).toList();

                if (fieldDeclaration.isStatic()){
                    if (fieldDeclaration.isPublic()){
                        cacheType.getPublicStaticFields().addAll(variables);

                    }
                    else if (!fieldDeclaration.isPrivate() && !fieldDeclaration.isProtected()){
                        cacheType.getDefaultStaticFields().addAll(variables);
                    }
                }
                else {
                    if (fieldDeclaration.isPublic()){
                        cacheType.getPublicFields().addAll(variables);

                    }
                    else if (!fieldDeclaration.isPrivate() && !fieldDeclaration.isProtected()){
                        cacheType.getDefaultFields().addAll(variables);
                    }
                }


            });



        });


        return file;
    }

    // анализируем текущий контекст кода, предлагаем только то, что доступно по контексту и соответствует введенным символам
    private BasicSuggestionContextBasedInfo contextBasedAnalysis(EditorBasicSuggestionRequest request) throws Exception{
        BasicSuggestionContextBasedInfo info = new BasicSuggestionContextBasedInfo();
        // Готовим строку к парсингу
        String preparedCode = CodeAnalysisUtils
                .prepareCode(request);
        // формируем AST древо для анализа
        CompilationUnit compilationResult = parser.parse(preparedCode).getResult()
                .orElseThrow(()->new IllegalStateException("parsing error"));

        // извлекаем информацию о package
        String packageDeclaration = CodeAnalysisUtils
                .extractPackage(compilationResult);

        info.setPackageWay(packageDeclaration);

        // анализируем присутствующие в коде типы, сразу заносим их названия в ответ, вычисляем тип, внутри которого находится юзер
        Optional<TypeDeclaration<?>> typeChosenByUser = CodeAnalysisUtils
                .collectAndCheckTypes(compilationResult, info, request);



        // если пользователь находится вне типа - ему доступен только набор ключевых слов
        // делаем пометку, чтобы дальнейшая сборка учитывала этот контекст
        if (typeChosenByUser.isEmpty()){
            info.setOutsideOfType(true);
            info.setKeywords(CodeAnalysisUtils.availableKeywordsOutsideOfType
                    .stream()
                    .filter(el->el.startsWith(request.getText()))
                    .toList());
            return info;
        }

        /*
        пользователь находится внутри типа
        Тут имеем следующие сценарии - позиция в теле класса (поля), в теле метода, в теле конструктора
        При работе с методом учитываем статический контекст

         */

        TypeDeclaration<?> type = typeChosenByUser.get();

        // для начала собираем всю необходимую информацию о типе
        List<String> staticMethods = new ArrayList<>();
        List<String> staticFields = new ArrayList<>();
        List<String> nonStaticFields = new ArrayList<>();
        List<String> nonStaticMethods = new ArrayList<>(CodeAnalysisUtils.objectMethods.stream() // сразу добавляем методы Object
                .filter(el -> el.startsWith(request.getText())).toList());

        // анализируем поля класса, извлекаем названия переменных, отмечаем статичность
        CodeAnalysisUtils.collectAndSplitFields(type, request.getText(), staticFields,nonStaticFields);




        // анализируем методы, при этом отмечаем его диапазон, смотрим, входит ли в него позиция юзера
        Optional<MethodDeclaration> methodChosenByUser = CodeAnalysisUtils.collectAndCheckMethods(
                type,request,staticMethods,nonStaticMethods
        );


        // юзер находится внутри метода
        if (methodChosenByUser.isPresent()){
            MethodDeclaration method = methodChosenByUser.get();

            // добавляем доступные ключевые слова
            info.setKeywords(CodeAnalysisUtils.availableKeywordsForMethodAndConstructor
                    .stream().filter(el->el.startsWith(request.getText())).toList());

            /*
            добавляем методы и поля
            если метод статичен, то доступен только static контекст
             */
            if (!method.isStatic()){
                info.getMethods().addAll(nonStaticMethods);
                info.getFields().addAll(nonStaticFields);
            }

            info.getMethods().addAll(staticMethods);
            info.getFields().addAll(staticFields);


            // анализируем параметры и тело метода, вычленяем локальные переменные с учетом позиции юзера
            info.getLocalVariables()
                    .addAll(CodeAnalysisUtils.extractLocalVariablesAndParameters(method, request));


            return info;

        }

        // юзер находится вне какого либо метода - он в теле класса или в конструкторе

        // проверка на нахождение внутри конструктора
        Optional<ConstructorDeclaration> constructorChosenByUser = CodeAnalysisUtils
                .collectAndCheckConstructors(type,request);

        // пользователь внутри конструктора
        if (constructorChosenByUser.isPresent()){
            // добавляем доступные ключевые слова
            info.setKeywords(CodeAnalysisUtils.availableKeywordsForMethodAndConstructor
                    .stream().filter(el->el.startsWith(request.getText())).toList());

            // добавляем как статичный, так и обычный контекст
            info.getMethods().addAll(staticMethods);
            info.getFields().addAll(staticFields);
            info.getMethods().addAll(nonStaticMethods);
            info.getFields().addAll(nonStaticFields);

            // анализируем параметры и тело метода, вычленяем локальные переменные с учетом позиции юзера
            info.getLocalVariables()
                    .addAll(CodeAnalysisUtils
                            .extractLocalVariablesAndParameters(constructorChosenByUser.get(), request));
            return info;

        }

        // пользователь снаружи метода или конструктора
        // добавляем как статичный, так и обычный контекст
        info.getMethods().addAll(staticMethods);
        info.getFields().addAll(staticFields);
        info.getMethods().addAll(nonStaticMethods);
        info.getFields().addAll(nonStaticFields);






















        return info;
    }













}
