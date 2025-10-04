package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.projects.DirectoryDTO;
import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.exceptions.ProjectNotFoundException;
import com.mytry.editortry.Try.model.Directory;

import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.model.enums.FileStatus;
import com.mytry.editortry.Try.repository.DirectoryRepository;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.repository.UserRepository;
import com.mytry.editortry.Try.utils.cache.CacheSystem;
import com.mytry.editortry.Try.utils.processes.ProjectLogger;
import com.mytry.editortry.Try.utils.projects.ProjectConstructor;
import com.mytry.editortry.Try.utils.projects.ProjectType;
import com.mytry.editortry.Try.utils.websocket.raw.WebSocketLogger;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;


import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
public class ProjectService {


    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ProjectLogger projectLogger;

    @Autowired
    private CacheSystem cacheSystem;

    @Autowired
    private ProjectConstructor projectConstructor;



    // логгер
    @Autowired
    private WebSocketLogger webSocketLogger;



    @Value("${files.directory}")
    private String disk_location_user_filebase;

    @Value("${common.directory}")
    private String disk_location_common_system_directory;



    // загрузка дерева проекта
    public ProjectDTO loadProjectByUsernameAndName(String username, String name){

        return mapTree(projectRepository.findByOwnerUsernameAndName(username, name).orElseThrow(ProjectNotFoundException::new));
    }








    // создание нового проекта с директорией (архитектура будет позволять посылать запрошенный тип проекта)
    @Transactional(rollbackOn = Exception.class)
    public void createProject(String username, String projectName) throws Exception {



        Project project = new Project();
        project.setName(projectName);
        project.setCreatedAt(Instant.now());

        User user = userRepository
                .findWithProjectsByUsername(username).orElseThrow(()->new UsernameNotFoundException("user doesn't exists"));

        boolean projectExists = user.getProjects().stream()
                .anyMatch(p -> p.getName().equals(projectName));

        if (projectExists) {
            throw new IllegalArgumentException("Project already exists");
        }

        Directory root = new Directory();

        root.setImmutable(true); // важно - корневая папка строго иммутабельна

        root.setName(projectName);
        root.setCreatedAt(Instant.now());
        directoryRepository.save(root);

        project.setRoot(root);

        project.setOwner(user);
        user.getProjects().add(project);

        projectRepository.save(project);



        // сохранение директории на диске
        java.io.File dir = new java.io.File(disk_location_user_filebase +username+"/projects/"+projectName);
        System.out.println(dir.getAbsolutePath());
        if (!dir.exists()){
            boolean result  = dir.mkdir();
            if(!result){
                throw new IllegalArgumentException("directory didn't created");
            }
        }

        else {
            throw new IllegalArgumentException("directory already exists");
        }







        try {
            // кешируем путь к проекту
            root.setConstructedPath(dir.getAbsolutePath());
            projectConstructor.buildProject(root, ProjectType.MAVEN_CLASSIC);
        }
        catch (Exception e){
            e.printStackTrace();
            // подчищаем
            Files.delete(dir.toPath()); // todo тут все рекурсивно

            // выброс исключения откатывает все транзакции
            throw new IllegalStateException("build failed "+e.getMessage());
        }

    }

    // todo удаление проекта реализуется через корзину, поэтому пока не трогаем этот пункт
    @Transactional
    public void deleteProject(Long id){
        if (!projectRepository.existsById(id)){
            throw new EntityNotFoundException("no found");
        }
        projectRepository.deleteById(id);
    }



    /*
    создание директории внутри проекта

    Операция с кешем - удаление кеша, если он есть

     */

    @Transactional(rollbackOn = Exception.class)
    public void createDirectory(String username, String projectName, String index, String suggestedDirectoryName){


        Project project = projectRepository.findByOwnerUsernameAndName(username, projectName)
                .orElseThrow(ProjectNotFoundException::new);

        String fullPath = disk_location_user_filebase +"/"+username+"/projects/";
        Directory parent = null;

        if (index.equals("basic_root")){
            // работаем с корневой папкой проекта
            fullPath = fullPath+projectName+"/"+suggestedDirectoryName;
            parent = project.getRoot();
        }

        else {

            Long id = Long.parseLong(index.split("_")[1]);
            // наша цель - сгенерировать путь до директории, в которой мы будем создавать новую директорию
            List<String> way = new ArrayList<>();



            parent = generateWay(project.getRoot(), id, way);




            if (parent == null){
                throw new IllegalArgumentException();
            }

            StringBuilder stringBuilder = new StringBuilder(fullPath);

            for (String s:way){
                stringBuilder.append(s);
                stringBuilder.append("/");
            }
            fullPath = stringBuilder+suggestedDirectoryName;
        }

        // мы сохраняем directory сущность, после чего в файловой системе создаем соответсвующую папку согласно fullPath

        // проверяем уникальность имени


        for (Directory d:parent.getChildren()){
            if (d.getName().equals(suggestedDirectoryName)){
                throw new IllegalArgumentException("not unique");
            }
        }
        // пишем в базу
        Directory toCreate = new Directory();
        toCreate.setName(suggestedDirectoryName);
        toCreate.setCreatedAt(Instant.now());
        parent.getChildren().add(toCreate);
        toCreate.setParent(parent);

        directoryRepository.save(toCreate);



        // пишем на диск


        // сохранение директории на диске
        java.io.File dir = new java.io.File(fullPath);
        System.out.println(dir.getAbsolutePath());
        if (!dir.exists()){
            boolean result  = dir.mkdir();
            if(!result){
                throw new IllegalArgumentException("directory didn't created");
            }
        }

        else {
            throw new IllegalArgumentException("directory already exists");
        }


        // подчищаем неактуальный кеш (но не удаляем саму сущность)
        cacheSystem.clearProjectCacheContent(project.getId());




    }



    /* удаление директории внутри проекта

    Операция с кешем - удаление кеша

     */
    @Transactional(rollbackOn = Exception.class)
    public void deleteDirectory(String username, String projectName, String index)  {


        // проверяем, существует ли проект
        Project project = projectRepository.findByOwnerUsernameAndName(username, projectName).orElseThrow(
                ()-> new IllegalArgumentException("project doesn't exists")
        );

        // извлекаем id директории, полученный с запроса
        Long id = Long.parseLong(index.split("_")[1]);

        // извлекаем сущность директории из базы данных
        Directory directory = directoryRepository.findById(id).orElseThrow(IllegalArgumentException::new);

        if (directory.getParent()==null){
            throw new IllegalArgumentException("this is root");
        }

        if (directory.isImmutable()){
            throw new IllegalArgumentException("you can't delete project structure folder");
        }




        // конструируем путь до директории
        ArrayDeque<String> way = new ArrayDeque<>();
        Directory parent = directory.getParent();



        // начиная с файла, добираемся до корневой папки
        Long parentId=-1L;
        while (parent!=null){
            parentId = parent.getId();
            way.addFirst("/"+parent.getName());
            parent = parent.getParent();
        }
        way.add("/");
        StringBuilder sb = new StringBuilder("/"+username+"/projects/");
        way.forEach(sb::append);

        String fullPath = disk_location_user_filebase +sb+directory.getName()+"/";


        // если найденный parentId не совпадает с root id проекта - то существует нарушение логики в базе данных
        if (!Objects.equals(parentId, project.getRoot().getId())){
            throw new IllegalArgumentException("Invalid file path");
        }


        // собираем все вложенные директории и файлы для удаления их из базы данных

        ArrayDeque<Directory> directoriesToDelete = new ArrayDeque<>();
        ArrayDeque<File> filesToDelete = new ArrayDeque<>();

        collectChildren(directory, directoriesToDelete, filesToDelete);



        while(!filesToDelete.isEmpty()){
            fileRepository.delete(filesToDelete.removeFirst());
        }

        while (!directoriesToDelete.isEmpty()){
            directoryRepository.delete(directoriesToDelete.removeFirst());
        }



        // удаление директории с диска происходит также рекурсивно - мы должны сначала удалить вложения,
        // а потом их родителей. Просто так можно удалить только пустую папку
        try {
            FileSystemUtils.deleteRecursively(Path.of(fullPath));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // чистим неактуальный кеш проекта
        cacheSystem.clearProjectCacheContent(project.getId());


    }



    /* Создание файла для проекта

    Операция с кешем - удаляем кеш

     */
    @Transactional(rollbackOn = Exception.class)
    public void createFile(String username, String projectName, String index, String suggestion) throws Exception {

        String props[] = suggestion.split("\\.");
        String filename = props[0];
        String extension = props[1];

        Project project = projectRepository.findByOwnerUsernameAndName(username, projectName)
                .orElseThrow(ProjectNotFoundException::new);

        String fullPath = disk_location_user_filebase +"/"+username+"/projects/";
        Directory parent = null;

        if (index.equals("basic_root")){
            // работаем с корневой папкой проекта
            fullPath = fullPath+projectName+"/";
            parent = project.getRoot();
        }

        else {
            // извлекаем id директории, в которой мы планируем создать файл
            Long id = Long.parseLong(index.split("_")[1]);
            // наша цель - сгенерировать путь до директории
            List<String> way = new ArrayList<>();



            parent = generateWay(project.getRoot(), id, way);




            if (parent == null){
                throw new IllegalArgumentException();
            }

            StringBuilder stringBuilder = new StringBuilder(fullPath);

            for (String s:way){
                stringBuilder.append(s);
                stringBuilder.append("/");
            }
            fullPath = stringBuilder.toString(); //  "/.../directory/" format
        }

        System.out.println(fullPath);
        for (File file: parent.getFiles()){
            if (file.getName().equals(filename)){
                throw new IllegalArgumentException("not unique name for file");
            }
        }

        
        File toCreate = new File();
        toCreate.setExtension(extension);
        toCreate.setCreatedAt(Instant.now());
        toCreate.setUpdatedAt(Instant.now());
        toCreate.setName(filename);
        toCreate.setParent(parent);
        parent.getFiles().add(toCreate);
        fileRepository.save(toCreate);

        try{
            Files.createFile(Path.of(fullPath+filename+"."+extension));





            // автоматически генерируем package и класс;
            if (extension.equals("java")){
                String[] packageWay = fullPath.split("src/main/java/");
                StringBuilder packageName = new StringBuilder();
                // не папка java
                if (packageWay.length>1){

                    packageName.append("package ");


                    // на случай. если кто-то смышленный создаст еще один src/main/java снизу иерархии
                    for (int x = 1; x<packageWay.length; x++){
                        String part = packageWay[x];
                        part = part.replace("/", ".");
                        packageName.append(part);
                    }
                    packageName.deleteCharAt(packageName.length()-1);
                    packageName.append(";");


                }
                String template = """
                        %s
                        
                        public class %s{
                        
                        }
                        """
                        .formatted(packageName.toString(),filename);


                try (FileWriter writer = new FileWriter(fullPath+filename+"."+extension)) {
                    writer.write(template);

                } catch (IOException e) {
                    throw new IllegalArgumentException("error while writing java file template");
                }

            }

        }
        catch (Exception e){
            webSocketLogger.log(e.getMessage());
            throw new Exception(e.getMessage());
        }

        // чистим кеш - он потерял актуальность
        cacheSystem.clearProjectCacheContent(project.getId());
    }


    /* удаление файла в проекте

    ограничение - если файл имеет активного подписчика, имеющего доступ к редактированию и сохранению
    ограничение - если файл иммутабелен
    особый случай - файл - точка запуска проекта - нужно сделать пометку об этом перед удалением

    операция с кешем - удаление

     */
    @Transactional(rollbackOn = Exception.class)
    public void deleteFile(String username, String projectName, String index) throws Exception{

        // проверяем, существует ли проект
        Project project = projectRepository.findByOwnerUsernameAndName(username, projectName).orElseThrow(
                ()-> new IllegalArgumentException("project doesn't exists")
        );

        // извлекаем id файла, полученный с запроса
        Long id = Long.parseLong(index.split("_")[1]);

        // извлекаем сущность файла из базы данных
        File file = fileRepository.findById(id).orElseThrow(()->new IllegalArgumentException("file doesn't exists"));

        if (file.isImmutable()){
            throw new IllegalArgumentException("you can't remove this file");
        }


        // конструируем путь до файла
        ArrayDeque<String> way = new ArrayDeque<>();
        Directory parent = file.getParent();

        // файл не может быть "бесхозным"
        if (parent==null){
            throw new IllegalArgumentException("no parent directory");
        }

        // начиная с файла, добираемся до корневой папки
        Long parentId=-1L;
        while (parent!=null){
            parentId = parent.getId();
            way.addFirst("/"+parent.getName());
            parent = parent.getParent();

        }
        way.add("/");
        StringBuilder sb = new StringBuilder("/"+username+"/projects/");
        way.forEach(sb::append);

        String fullPath = disk_location_user_filebase +sb+file.getName()+"."+file.getExtension();


        // если найденный parentId не совпадает с root id проекта - то существует нарушение логики в базе данных
        if (!Objects.equals(parentId, project.getRoot().getId())){
            throw new IllegalArgumentException("Invalid file path");
        }



        //тут все проверки выполнены - переходим к конкретным изменениям в бд и на диске

        fileRepository.delete(file);
        Files.delete(Path.of(fullPath));

        // подчищаем кеш - он потерял актуальность
        cacheSystem.clearProjectCacheContent(project.getId());



    }





    // собираем ссылки на все поддиректории и файлы при удалении из базы данных - удаление происходит с нижних элементов (детей)

    private void collectChildren(Directory directory, ArrayDeque<Directory> directories, ArrayDeque<File> files){

        for (Directory d:directory.getChildren()){
            collectChildren(d, directories, files);
        }

        files.addAll(directory.getFiles());

        directories.add(directory);
    }



    // генерируем ссылку от "кандидата" до искомого айди (сверху вниз)
    private Directory generateWay(Directory candidate,
                                  Long directoryId,
                                  List<String> way){

        if (directoryId.equals(candidate.getId())){

            way.add(candidate.getName());
            return candidate;
        }

        else {
            if (!candidate.getChildren().isEmpty()){
                way.add(candidate.getName());

                for (Directory d:candidate.getChildren()){
                    Directory found = generateWay(d, directoryId, way);
                    if (found!=null) {
                        return found;
                    }
                }

                way.remove(way.size()-1);
                return null;




            }
            else {
                return null;
            }
        }

    }













    // генерируем репрезентацию проекта для сервера (дерева) с помощью traverse

    private ProjectDTO mapTree(Project project){


        Directory root = project.getRoot();

        DirectoryDTO rootDTO = new DirectoryDTO();

        Map<String, FlatTreeMember> flatTree = new HashMap<>(); // плоская структура


        traverse(root, rootDTO, null, flatTree);


        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setName(project.getName());
        projectDTO.setId(project.getId());

        projectDTO.setRoot(rootDTO);
        projectDTO.setFlatTree(flatTree);

        projectDTO.setRunning(project.isRunning());

        try{
            String folderPath = disk_location_user_filebase +project.getOwner().getUsername()+"/projects/"+project.getName()+"/";
            List<String> logLines = projectLogger.loadLogLines(project.getId(), folderPath);
            projectDTO.setProjectLogLines(logLines);

        }
        catch (Exception e){
            e.printStackTrace();
        }


        return projectDTO;

    }

    // проходим по директория deep-first-traversal, готовя dto

    private void traverse(Directory directory, DirectoryDTO dto,
                          ArrayList<DirectoryDTO> layer,
                          Map<String, FlatTreeMember> flatTree){

        if (directory.isHidden()) return;



        dto.setName(directory.getName());
        dto.setId(directory.getId());

        FlatTreeMember directoryMember = new FlatTreeMember();
        directoryMember.setIndex("directory_"+directory.getId());
        directoryMember.setData(directory.getName());
        directoryMember.setFolder(true);
        directoryMember.setImmutable(directory.isImmutable());



        if (layer!=null){
            layer.add(dto);

        }

        if (layer==null){
            // если корень
            directoryMember.setIndex("basic_root");


        }

        /*
        обработка файлов
         */

        if (directory.getFiles()!=null){
            for (File file:directory.getFiles()){
                // проверяем статус на случай, если какая-то операция над файлом "зависла"
                // также проверяем на видимость
                if (file.isHidden()) continue;
                if (file.getStatus()== FileStatus.AVAILABLE){
                    String index = "file_"+file.getId();
                    FlatTreeMember fileMember = new FlatTreeMember();
                    fileMember.setIndex(index);
                    fileMember.setData(file.getName()+"."+file.getExtension());
                    fileMember.setFolder(false);
                    fileMember.setImmutable(file.isImmutable());

                    directoryMember.getChildren().add(index);


                    flatTree.put(index, fileMember);
                }




            }
        }


        if (directory.getChildren().isEmpty()) {
            dto.setChildren(new ArrayList<>());
            //System.out.println("end reached");
        }

        else {
            ArrayList<DirectoryDTO> children = new ArrayList<>();
            dto.setChildren(children);

            for (Directory d:directory.getChildren()){
                String index = "directory_"+d.getId();
                traverse(d, new DirectoryDTO(), children, flatTree);
                directoryMember.getChildren().add(index);
            }
        }

        flatTree.put(directoryMember.getIndex(), directoryMember);
    }







}
