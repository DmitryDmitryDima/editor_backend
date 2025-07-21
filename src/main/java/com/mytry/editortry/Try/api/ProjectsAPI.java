package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.service.project.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{username}/projects/{projectname}")
public class ProjectsAPI {

    @Autowired
    private ProjectService projectService;


    @GetMapping
    public ResponseEntity<ProjectDTO> loadProjectTree(@PathVariable(name = "projectname") String projectname,
                                                      @PathVariable(name = "username") String username){



        return ResponseEntity.ok(projectService.loadProjectByUsernameAndName(username, projectname));

    }

    @PostMapping("/createDirectory/{index}/{suggestion}")
    public ResponseEntity<Void> createDirectory(@PathVariable(name = "projectname") String projectname,
                                                @PathVariable(name = "username") String username,
                                                @PathVariable("index") String index,
                                                @PathVariable("suggestion") String suggestion){

        projectService.createDirectory(username, projectname, index, suggestion);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/createFile/{index}/{suggestion}")
    public ResponseEntity<Void> createFile(
            @PathVariable(name = "projectname") String projectname,
            @PathVariable(name = "username") String username,
            @PathVariable("index") String index,
            @PathVariable("suggestion") String suggestion) throws Exception {

        projectService.createFile(username, projectname, index, suggestion);
        return ResponseEntity.noContent().build();
    }


    // мы не можем удалить корневую папку! поэтому извлечение id происходит по однозначному алгоритму

    @PostMapping("/removeDirectory/{index}")
    public ResponseEntity<Void> removeDirectory(
            @PathVariable(name = "projectname") String projectName,
            @PathVariable(name = "username") String username,
            @PathVariable("index") String index
    ){

        projectService.deleteDirectory(username, projectName,index);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/removeFile/{index}")
    public ResponseEntity<Void> removeFile(
            @PathVariable(name = "projectname") String projectName,
            @PathVariable(name = "username") String username,
            @PathVariable("index") String index
    ) throws Exception {


        projectService.deleteFile(username, projectName, index);

        return ResponseEntity.noContent().build();
    }


}
