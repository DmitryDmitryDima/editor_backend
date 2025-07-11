package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.projects.DirectoryDTO;
import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.exceptions.project.ProjectNotFoundException;
import com.mytry.editortry.Try.model.Directory;

import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.repository.DirectoryRepository;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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



    @Value("${files.directory}")
    private String disk_location;



    // удаление проекта реализуется через корзину, поэтому пока не трогаем этот пункт
    @Transactional
    public void deleteProject(Long id){
        if (!projectRepository.existsById(id)){
            throw new EntityNotFoundException("no found");
        }
        projectRepository.deleteById(id);
    }


    // создание нового проекта с директорией
    @Transactional(rollbackOn = IllegalArgumentException.class)
    public void createProject(String username, String projectName) throws IllegalArgumentException {

        /*
        вариант кода без каскадирования
         */

        Project project = new Project();
        project.setName(projectName);

        User user = userRepository
                .findWithProjectsByUsername(username).orElseThrow(()->new UsernameNotFoundException("user doesn't exists"));

        boolean projectExists = user.getProjects().stream()
                .anyMatch(p -> p.getName().equals(projectName));

        if (projectExists) {
            throw new IllegalArgumentException("Project already exists");
        }

        Directory root = new Directory();
        root.setName(projectName);

        directoryRepository.save(root);

        project.setRoot(root);

        project.setOwner(user);
        user.getProjects().add(project);

        projectRepository.save(project);






        createDirectoryAtDisk(username, projectName);

    }


    private void createDirectoryAtDisk(String username, String projectName) throws IllegalArgumentException{
        java.io.File dir = new java.io.File(disk_location+username+"/projects/"+projectName);
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

    }




    // имя проекта - уникально в пределах одного пользователя
    public ProjectDTO loadProjectByUsernameAndName(String username, String name){

        return mapTree(projectRepository.findByOwnerUsernameAndName(username, name).orElseThrow(ProjectNotFoundException::new));
    }


    @Transactional(rollbackOn = {IllegalArgumentException.class})
    public void deleteFile(String username, String projectName, String index) throws Exception{

        // проверяем, существует ли проект
        Project project = projectRepository.findByOwnerUsernameAndName(username, projectName).orElseThrow(
                ()-> new IllegalArgumentException("project doesn't exists")
        );

        // извлекаем id файла, полученный с запроса
        Long id = Long.parseLong(index.split("_")[1]);

        // извлекаем сущность файла из базы данных
        File file = fileRepository.findById(id).orElseThrow(()->new IllegalArgumentException("file doesn't exists"));

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

        String fullPath = disk_location+sb+file.getName()+"."+file.getExtension();


        // если найденный parentId не совпадает с root id проекта - то существует нарушение логики в базе данных
        if (!Objects.equals(parentId, project.getRoot().getId())){
            throw new IllegalArgumentException("Invalid file path");
        }


        /*
        тут все проверки выполнены - переходим к конкретным изменениям в бд и на диске

        1) Генерируем UUID для переименования
        2) Производим попытку переименования файла на диске.
         Компенсация - метод возвращает ошибку
        3) Сохраняем новое имя и статус для сущности в бд.
         Компенсация - создание compensation_query с кодом ошибки 'file_db_rename_before_removing_error'
         Запись содержит в себе следующую инфу - id файла в бд, новое имя
         Обработчик пробует переименовать запись в бд, в случае успеха совершает шаги ниже.

        4) Пробуем удалить файл с диска
          Компенсация - создание compensation_query с кодом ошибки 'file_disk_remove_error'
          Запись содержит в седе следующую инфу - путь к файлу (с новым именем), id файла в бд
          Обработчик пробует стереть файл, после чего реализует шаг 5
        5) Пробуем окончательно удалить файл из БД
           Компенсация - создание compensation_query с кодом ошибки 'file_db_removing_error'
           Запись содержит в себе id файла в бд
           Обработчик пробует стереть файл из бд


           таким образом, каждый из верхних шагов может продолжать алгоритм с той точки, где произошла остановка
           Соответствующим образом каждый из обработчиков может генерировать compensation_query при переходе на следующий шаг
           Осталось придумать, как сделать цепочку решений более элегантной, в ООП стиле
         */


        /*
        ниже - старая логика
         */

        fileRepository.delete(file);


        Files.delete(Paths.get(fullPath));



    }




    @Transactional(rollbackOn = IllegalArgumentException.class)
    public void deleteDirectory(String index){
        Long id = Long.parseLong(index.split("_")[1]);





        Directory directory = directoryRepository.findById(id).orElseThrow(IllegalArgumentException::new);
        if (directory.getParent()==null){
            throw new IllegalArgumentException("this is root");
        }

        ArrayDeque<Directory> directoriesToDelete = new ArrayDeque<>();
        ArrayDeque<File> filesToDelete = new ArrayDeque<>();

        collectChildren(directory, directoriesToDelete, filesToDelete);

        System.out.println(directoriesToDelete);
        System.out.println(filesToDelete);

        while(!filesToDelete.isEmpty()){
            fileRepository.delete(filesToDelete.removeFirst());
        }

        while (!directoriesToDelete.isEmpty()){
            directoryRepository.delete(directoriesToDelete.removeFirst());
        }





    }

    @Transactional(rollbackOn = IllegalArgumentException.class)
    public void createFile(String username, String projectName, String index, String suggestion){

        String props[] = suggestion.split("\\.");
        String filename = props[0];
        String extension = props[1];

        Project project = projectRepository.findByOwnerUsernameAndName(username, projectName)
                .orElseThrow(ProjectNotFoundException::new);

        String fullPath = disk_location+"/"+username+"/projects/";
        Directory parent = null;

        if (index.equals("basic_root")){
            // работаем с корневой папкой проекта
            fullPath = fullPath+projectName+"/";
            parent = project.getRoot();
        }

        else {
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
            fullPath = stringBuilder.toString();
        }

        System.out.println(fullPath);
        for (File file: parent.getFiles()){
            if (file.getName().equals(filename)){
                throw new IllegalArgumentException("not unique name for file");
            }
        }

        
        File toCreate = new File();
        toCreate.setExtension(extension);
        toCreate.setName(filename);
        toCreate.setParent(parent);
        parent.getFiles().add(toCreate);
        fileRepository.save(toCreate);
    }



    @Transactional(rollbackOn = IllegalArgumentException.class)
    public void createDirectory(String username, String projectName, String index, String suggestedDirectoryName){


        Project project = projectRepository.findByOwnerUsernameAndName(username, projectName)
                .orElseThrow(ProjectNotFoundException::new);

        String fullPath = disk_location+"/"+username+"/projects/";
        Directory parent = null;

        if (index.equals("basic_root")){
            // работаем с корневой папкой проекта
            fullPath = fullPath+projectName+"/"+suggestedDirectoryName;
            parent = project.getRoot();
        }

        else {

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
        parent.getChildren().add(toCreate);
        toCreate.setParent(parent);

        directoryRepository.save(toCreate);











    }

    // собираем ссылки на все поддериктории и файлы при удалении

    private void collectChildren(Directory directory, ArrayDeque<Directory> directories, ArrayDeque<File> files){

        for (Directory d:directory.getChildren()){
            collectChildren(d, directories, files);
        }

        files.addAll(directory.getFiles());

        directories.add(directory);
    }

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









    // проходим по директория deep-first-traversal, готовя dto

    private void traverse(Directory directory, DirectoryDTO dto,
                          ArrayList<DirectoryDTO> layer,
                          Map<String, FlatTreeMember> flatTree){

        dto.setName(directory.getName());
        dto.setId(directory.getId());

        FlatTreeMember directoryMember = new FlatTreeMember();
        directoryMember.setIndex("directory_"+directory.getId());
        directoryMember.setData(directory.getName());
        directoryMember.setFolder(true);
        directoryMember.setCanMove(true);
        directoryMember.setCanRename(true);



        if (layer!=null){
            layer.add(dto);

        }

        if (layer==null){
            // если корень
            directoryMember.setIndex("basic_root");
            directoryMember.setCanMove(false);
            directoryMember.setCanRename(false);

        }

        /*
        обработка файлов
         */

        if (directory.getFiles()!=null){
            for (File file:directory.getFiles()){
                String index = "file_"+file.getId();
                FlatTreeMember fileMember = new FlatTreeMember();
                fileMember.setIndex(index);
                fileMember.setData(file.getName()+"."+file.getExtension());
                fileMember.setFolder(false);
                fileMember.setCanMove(true);
                fileMember.setCanRename(true);

                directoryMember.getChildren().add(index);


                flatTree.put(index, fileMember);



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


    private ProjectDTO mapTree(Project project){

        //System.out.println(project.getRoot().getChildren()+" children"); // пустой список
        //System.out.println(project.getRoot().getParent()+" parent"); // null


        Directory root = project.getRoot();

        DirectoryDTO rootDTO = new DirectoryDTO();

        Map<String, FlatTreeMember> flatTree = new HashMap<>(); // плоская структура


        traverse(root, rootDTO, null, flatTree);





        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setName(project.getName());
        projectDTO.setId(project.getId());

        projectDTO.setRoot(rootDTO);
        projectDTO.setFlatTree(flatTree);















        return projectDTO;





    }





}
