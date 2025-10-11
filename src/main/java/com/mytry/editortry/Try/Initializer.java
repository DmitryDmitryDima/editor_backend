package com.mytry.editortry.Try;


import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.repository.DirectoryRepository;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class Initializer implements CommandLineRunner {


    private final List<String> immutables = List.of("src", "main","java","resources","com");
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    @Autowired
    private FileRepository fileRepository;

    @Value("${files.directory}")
    private String disk_location;

    // для теста заполняем базу данных, основываясь на текущем состоянии файловой системы
    private void travelAndPersistWorkingDirectory(){

        String username = "dima";
        User user = new User();
        user.setUsername(username);


        userRepository.save(user);

        String projectsDirectory = disk_location+username+"/projects/java/";

        java.io.File projectsFolder = new java.io.File(projectsDirectory);

        java.io.File[] projects = projectsFolder.listFiles();
        if (projects==null){
            return;
        }

        for (java.io.File f:projects){
            if (f.isDirectory()){

                Project project = new Project();
                project.setName(f.getName());
                project.setOwner(user);
                project.setCreatedAt(Instant.now());
                Directory root = new Directory();
                project.setRoot(root);
                root.setName(f.getName());
                root.setCreatedAt(Instant.now());
                root.setImmutable(true);


                prepareMyRoot(f, root);
                directoryRepository.save(root);
                project.setRoot(root);
                project.setOwner(user);
                projectRepository.save(project);

            }
        }



    }

    private void prepareMyRoot(java.io.File root, Directory parentEntity){

        parentEntity.setChildren(new ArrayList<>());
        parentEntity.setFiles(new ArrayList<>());

        java.io.File[] childrenFiles = root.listFiles();

        if (childrenFiles!=null){
            for (java.io.File f:childrenFiles){
                if (f.isDirectory()){
                    String name = f.getName();


                    Directory child = new Directory();
                    // для удобства введем запрет на удаление папок с определенным именем
                    if (immutables.contains(name)){
                        child.setImmutable(true);
                    }

                    // таргет папка невидима
                    if(name.equals("target")){
                        child.setHidden(true);
                    }
                    child.setName(name);
                    child.setCreatedAt(Instant.now());
                    parentEntity.getChildren().add(child);
                    child.setParent(parentEntity);
                    prepareMyRoot(f, child);

                }

                else {

                    File file = new File();
                    String fullName = f.getName();
                    int lastIndex = fullName.lastIndexOf(".");

                    // лог просто так не увидеть
                    if (f.getName().equals("current.log") || f.getName().equals("Dockerfile") ){
                        file.setHidden(true);
                    }

                    if (lastIndex>0){
                        file.setName(f.getName().substring(0,lastIndex));
                        file.setExtension(f.getName().substring(lastIndex+1));
                    }
                    else {
                        file.setName(f.getName());
                        file.setExtension("");
                    }


                    if(fullName.equals("pom.xml")){
                        file.setImmutable(true);
                    }

                    file.setParent(parentEntity);
                    file.setCreatedAt(Instant.now());
                    file.setUpdatedAt(Instant.now());
                    parentEntity.getFiles().add(file);
                }


            }
        }




    }

    @Override
    public void run(String... args) throws Exception {

        travelAndPersistWorkingDirectory();







    }
}
