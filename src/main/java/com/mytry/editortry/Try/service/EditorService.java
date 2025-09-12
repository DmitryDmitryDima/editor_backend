package com.mytry.editortry.Try.service;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
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
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectFile;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionOuterProjectFile;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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





    // метод, где пересобирается (создается) кеш для всего проекта
    private ProjectCacheDTO prepareCache(Long projectId, ProjectCache projectCache){
        // формируем путь к папке проекта на основании owner id и project id
        Project project = projectRepository
                .findById(projectId).orElseThrow(()-> new IllegalArgumentException("project not found"));
        String username = project.getOwner().getUsername();
        String projectname = project.getName();

        // получаем директорию com, проверяем, соответствует ли проект структуре maven
        Directory currentDirectory = project.getRoot();
        for (String structureFolderName:CodeAnalysisUtils.mavenFolderStructure){
            Optional<Directory> candidate = currentDirectory
                    .getChildren().stream().filter(directory->directory.getName().equals(structureFolderName)).findAny();
            if (candidate.isEmpty()) throw new IllegalArgumentException("invalid project structure");
            currentDirectory = candidate.get();

        }

        // путь к корневой папке проекта пользователя
        String javaPath = disk_directory+ username+"/projects/"+projectname+"/src/main/java";
        ProjectCacheDTO constructedCache = analyzeProject(currentDirectory, javaPath);
        projectCache.updateProjectCache(constructedCache);
        return constructedCache;
    }


    /*
     - формирование подсказки при вводе точки, к примеру после переменной
     - в этом методе - одна из точек формирования кеша (в случае его отсутствия)
     */

    public EditorDotSuggestionAnswer dotSuggestion(EditorDotSuggestionRequest request){
        EditorDotSuggestionAnswer answer = new EditorDotSuggestionAnswer();


        // редактируем код так, чтобы он был приятен парсеру
        String preparedCode = CodeAnalysisUtils.prepareCode(request);

        // Получаем ast древо
        CompilationUnit compilationResult = compile(preparedCode);

        // получаем список импортов из ast дерева
        List<String> imports = CodeAnalysisUtils.extractImports(compilationResult);

        // вычисляем package для контроля доступности полей
        String packageWay = CodeAnalysisUtils.extractPackage(compilationResult);

        // вычисляем выражение, в котором находится пользователь
        Optional<FieldAccessExpr> expressionCandidate = CodeAnalysisUtils.getExpression(compilationResult, request);

        // выражение не найдено - возвращаем пустой ответ
        if (expressionCandidate.isEmpty()){
            return answer;
        }

        Expression expression = expressionCandidate.get().getScope();
        // в начале пытаемся анализировать тип так, словно он находится (задан) внутри файла, или же доступен рефлексии
        try {
            CodeAnalysisUtils.analyzeExpressionAsInnerType(expression, answer);


        }
        catch (Exception e){
            /*
            проверяем наличие кеша
            */
            ProjectCache projectCache = cacheSystem.getProjectCache(request.getProject_id());


            ProjectCacheDTO projectCacheState = projectCache.getAWholeCache();


            // если кеш пустой, мы должны его пересобрать
            if (projectCacheState.getPackageToFileAssociation().isEmpty() && projectCacheState.getIdToFileAssociation().isEmpty()) {

                projectCacheState = prepareCache(request.getProject_id(), projectCache);
            }

            // пробуем соотнести информацию с импорта и тип
            // todo в дальнейшем нужно будет разработать механизм обработки цепочки, а также вызова метода из статического класса

            String rootType = null;
            for (VariableDeclarator vr:compilationResult.findAll(VariableDeclarator.class)){
                if (vr.getNameAsString().equals(expression.toString())){

                    rootType = vr.getTypeAsString();
                }
            }

            // одноименная переменная не найдена
            if (rootType==null){
                return answer;
            }

            String matchingImport = null;
            for (String importStatement:imports){
                if (importStatement.endsWith("."+rootType)){
                    matchingImport = importStatement;
                }
            }
            // соответствующий импорт отсутствует
            if (matchingImport == null){
                return answer;
            }

            // формируем package путь для найденного импорта
            String way = matchingImport.substring(0, matchingImport.length()-rootType.length()-1);

            List<CacheSuggestionInnerProjectFile> files = projectCacheState.getPackageToFileAssociation().get(way);


            // todo довольно уродливый кусок кода, позже переделать
            for (CacheSuggestionInnerProjectFile file:files){
                // проверка на парсинг
                if (file.isParsed() && file.getPublicType().getName().equals(rootType)){

                    answer.setMethods(file.getPublicType().getPublicMethods());
                    answer.setFields(file.getPublicType().getPublicFields());
                }
                // парсинг не проведен
                else if (!file.isParsed()) {
                    CacheSuggestionInnerProjectFile newAttempt =
                            generateInnerProjectFileAPI(CodeAnalysisUtils.readFileFromDisk(file.getFilePath()),
                                    file.getFilePath()) ;
                    if (newAttempt.isParsed() && file.getPublicType().getName().equals(rootType)){
                        // обновляем структуру
                        file.updateTypeStructureFrom(newAttempt);
                        file.setParsed(true);
                        answer.setMethods(file.getPublicType().getPublicMethods());
                        answer.setFields(file.getPublicType().getPublicFields());

                    }
                }






            }










        }


        return answer;
    }


    /*
     - формирование подсказки при вводе некоторого текстового фрагмента пользователем
     - в данном методе - одна из точек формирования кеша (в случае его отсутствия)
     */
    public EditorBasicSuggestionAnswer basicSuggestion(EditorBasicSuggestionRequest request){

        EditorBasicSuggestionAnswer answer = new EditorBasicSuggestionAnswer();

        /*
        Шаг 1 - Сбор контексто-ориентированной информации, учитывающей текущий код и позицию в нем
         */
        BasicSuggestionContextBasedInfo context;
        try {
            context = contextBasedAnalysis(request);
            answer.setContextBasedInfo(context);
        }
        catch (Exception e){
            throw new RuntimeException("invalid code context");
        }

        /*
        Шаг 2 - Формирование/получение внешнего кеша. После этого производим его анализ на соответствие контексту
         */


        ProjectCache projectCache = cacheSystem.getProjectCache(request.getProject_id());


        ProjectCacheDTO projectCacheState = projectCache.getAWholeCache();


        // если кеш пустой, мы должны его пересобрать
        if (projectCacheState.getPackageToFileAssociation().isEmpty() && projectCacheState.getIdToFileAssociation().isEmpty()) {

            projectCacheState = prepareCache(request.getProject_id(), projectCache);
        }

        // работаем со слепком кеша - извлекаем внутренние типы



        List<BasicSuggestionType> projectTypes = new ArrayList<>();
        Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation = projectCacheState.getPackageToFileAssociation();
        for (Map.Entry<String, List<CacheSuggestionInnerProjectFile>> entry:packageToFileAssociation.entrySet()){

            List<CacheSuggestionInnerProjectFile> files = entry.getValue();


            for (var f:files){



                if (f.isParsed() && f.getPublicType().getName().startsWith(request.getText())){
                    BasicSuggestionType basicSuggestionType = new BasicSuggestionType();
                    // формируем импорт только в случае, если не совпадает package
                    if (!context.getPackageWay().equals(f.getPackageWay())){
                        basicSuggestionType.setPackageWay(f.getPackageWay());
                    }

                    basicSuggestionType.setName(f.getPublicType().getName());
                    projectTypes.add(basicSuggestionType);
                }

                // пробуем пересобрать файл и снова извлечь тип
                else if (!f.isParsed()){
                    CacheSuggestionInnerProjectFile newAttempt =
                            generateInnerProjectFileAPI(CodeAnalysisUtils.readFileFromDisk(f.getFilePath()),
                                    f.getFilePath()) ;
                    if (newAttempt.isParsed() && f.getPublicType().getName().startsWith(request.getText())){
                        // обновляем структуру
                        f.updateTypeStructureFrom(newAttempt);
                        f.setParsed(true);
                        BasicSuggestionType basicSuggestionType = new BasicSuggestionType();
                        // формируем импорт только в случае, если не совпадает package
                        if (!context.getPackageWay().equals(f.getPackageWay())){
                            basicSuggestionType.setPackageWay(f.getPackageWay());
                        }

                        basicSuggestionType.setName(f.getPublicType().getName());
                        projectTypes.add(basicSuggestionType);
                    }
                }
            }
        }

        answer.setProjectTypes(projectTypes);





        // работаем с внешними библиотеками - пока что в рамках стандартной библиотеки java
        // внешние типы запрашиваются только если пользователь находится внутри контекста типа
        if (context.isOutsideOfType()){
            return answer;
        }

        // извлекаем read-only java cache, после чего конвертируем его в формат серверного ответа
        List<CacheSuggestionOuterProjectFile> javaTypes = cacheSystem
                .getStandartLibraryCache()
                .getTypesByFragment(request.getText());

        answer.setOuterTypes(CodeAnalysisUtils.convertCacheAnswerToBasicSuggestionType(javaTypes));

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
                CacheSuggestionInnerProjectFile fileAPI = generateInnerProjectFileAPI(request.getContent(), disk_path);
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




    // анализируем проект, рекурсивно исследуя его структуру
    public ProjectCacheDTO analyzeProject(Directory root, String rootPath){



        ProjectCacheDTO projectCacheDTO = new ProjectCacheDTO();
        try {

            // массив для динамической фиксации пути в зависимости от позиции в дереве (внутри рекурсии)
            ArrayList<String> layer = new ArrayList<>();
            layer.add(rootPath);



            collectAndAnalyzeFiles(layer, root, projectCacheDTO);

        }
        catch (Exception e){
            throw new RuntimeException("fail to construct project cache "+e.getMessage());
        }

        return projectCacheDTO;
    }

    /*
    вспомогательный метод для рекурсии - исследование директории и заполнение кеша
    root path + layer + filename = > полный путь
     */

    private void collectAndAnalyzeFiles(ArrayList<String> layer,
                                        Directory directory,
                                        ProjectCacheDTO dto) throws Exception{

        // формируем состояние пути в соответствии с позицией
        layer.add(directory.getName());

        // базовый путь для каждого из файлов
        // для каждого файла формируем строку пути
        StringBuilder filePath = new StringBuilder();

        for (String folderName:layer){
            filePath.append(folderName);
            filePath.append("/");
        }

        // начинаем с файлов - нас интересует java
        for (File fileEntity:directory.getFiles()){
            if (fileEntity.getExtension().equals("java")){




                // читаем файл
                String path = filePath
                        +fileEntity.getName()
                        +"."
                        +fileEntity.getExtension();

                String content = CodeAnalysisUtils.readFileFromDisk(path); // если ошибка чтения с диска - критическая ошибка

                // формируем api файла для кеша
                CacheSuggestionInnerProjectFile suggestion = generateInnerProjectFileAPI(content, path);

                // вносим полученный api в кеш ассоциации

                // id to file ассоциация
                dto.getIdToFileAssociation().put(fileEntity.getId(), suggestion);

                // package ассоциация - проверяем. затрагивался ли ранее package, если нет - создаем новый список
                Map<String, List<CacheSuggestionInnerProjectFile>> packages = dto.getPackageToFileAssociation();
                var packageList = packages.get(suggestion.getPackageWay());
                if (packageList == null){
                    ArrayList<CacheSuggestionInnerProjectFile> list = new ArrayList<>();
                    list.add(suggestion);
                    packages.put(suggestion.getPackageWay(), list);
                }
                else {
                    packageList.add(suggestion);
                }





            }

        }
        for (Directory ch:directory.getChildren()){
            collectAndAnalyzeFiles(layer, ch, dto);
        }

        // удаляем пройденную директорию из пути
        layer.remove(layer.size()-1);





    }




    // генерируем api для внутреннего файла проекта - public и default компоненты
    private CacheSuggestionInnerProjectFile generateInnerProjectFileAPI(String code, String filePath){

        CacheSuggestionInnerProjectFile file = new CacheSuggestionInnerProjectFile();

        try{
            CompilationUnit astTree = compile(code);
            file = CodeAnalysisUtils.generateInnerProjectFileAPI(astTree);
            file.setParsed(true);


        }
        // ошибка анализа кода
        catch (Exception e){
            file.setParsed(false); // для логики - явно указываем, что парсинг закончился ошибкой
        }

        finally {
            file.setFilePath(filePath);
        }


        return file;


    }






    // анализируем текущий контекст кода, предлагаем только то, что доступно по контексту и соответствует введенным символам
    private BasicSuggestionContextBasedInfo contextBasedAnalysis(EditorBasicSuggestionRequest request) throws Exception{
        BasicSuggestionContextBasedInfo info = new BasicSuggestionContextBasedInfo();
        // Готовим строку к парсингу
        String preparedCode = CodeAnalysisUtils
                .prepareCode(request);
        // формируем AST древо для анализа
        CompilationUnit compilationResult = compile(preparedCode);

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

    // создаем ast древо или выбрасываем ошибку
    private CompilationUnit compile(String code){
        return parser.parse(code).getResult()
                .orElseThrow(()->new IllegalStateException("parsing error"));
    }













}
