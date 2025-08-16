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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class Initializer implements CommandLineRunner {


    private List<String> immutables = List.of("src", "main","java","resources");
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

        String projectsDirectory = disk_location+username+"/projects/";

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
                    // таргет папка невидима (при создании проекта она не вносится в бд)
                    if(name.equals("target")){
                        continue;
                    }

                    Directory child = new Directory();
                    // для удобства введем запрет на удаление папок с определенным именем
                    if (immutables.contains(name)){
                        child.setImmutable(true);
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
        /*
        File file = new File();

        file.setExtension("java");
        file.setName("govno");
        file.setStatus(FileStatus.DELETING);
        fileRepository.save(file);

         */
        /*
        User user = new User();
        user.setUsername("dima");

        String projectName = "demo";
        String projectName1 = "demo2";

        Directory root = new Directory();
        root.setName(projectName);

        Directory root1 = new Directory();
        root1.setName(projectName1);

        File file = new File();
        file.setExtension("java");
        file.setName("BinarySearch");

        File file1 = new File();
        file1.setExtension("java");
        file1.setName("BinarySearch");



        file.setParent(root);

        List<File> rootFiles = new ArrayList<>();

        rootFiles.add(file);

        root.setFiles(rootFiles);




        Directory sub = new Directory();
        sub.setName("package");
        sub.setParent(root);

        Directory sub2 = new Directory();
        sub2.setName("package1");
        sub2.setParent(root);

        Directory sub3 = new Directory();
        sub3.setName("package3");
        sub3.setParent(sub);

        file1.setParent(sub3);

        List<File> sub3Files = new ArrayList<>();
        sub3Files.add(file1);
        sub3.setFiles(sub3Files);



        List<Directory> rootChildren = new ArrayList<>();
        rootChildren.add(sub);
        rootChildren.add(sub2);

        List<Directory> subChildren = new ArrayList<>();
        subChildren.add(sub3);

        sub.setChildren(subChildren);

        root.setChildren(rootChildren);

        directoryRepository.save(root);
        directoryRepository.save(root1);




        Project project = new Project();
        Project project1 = new Project();
        project.setName(projectName);
        project.setRoot(root);

        project1.setName(projectName1);
        project1.setRoot(root1);

        userRepository.save(user);
        project.setOwner(user);
        project1.setOwner(user);

        projectRepository.save(project);
        projectRepository.save(project1);

         */
    }
}
