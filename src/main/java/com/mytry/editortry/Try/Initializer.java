package com.mytry.editortry.Try;


import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.repository.DirectoryRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Initializer implements CommandLineRunner {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    @Override
    public void run(String... args) throws Exception {

        User user = new User();
        user.setUsername("dima");

        String projectName = "demo";

        Directory root = new Directory();
        root.setName(projectName);

        Directory sub = new Directory();
        sub.setName("package");
        sub.setParent(root);

        Directory sub2 = new Directory();
        sub2.setName("package1");
        sub2.setParent(root);

        Directory sub3 = new Directory();
        sub3.setName("package3");
        sub3.setParent(sub);

        List<Directory> rootChildren = new ArrayList<>();
        rootChildren.add(sub);
        rootChildren.add(sub2);

        List<Directory> subChildren = new ArrayList<>();
        subChildren.add(sub3);

        sub.setChildren(subChildren);

        root.setChildren(rootChildren);

        directoryRepository.save(root);




        Project project = new Project();
        project.setName(projectName);
        project.setRoot(root);

        userRepository.save(user);
        project.setOwner(user);

        projectRepository.save(project);
    }
}
