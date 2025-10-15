package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.projects.*;
import com.mytry.editortry.Try.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{username}/projects/java/{projectname}")
public class ProjectsAPI {

    @Autowired
    private ProjectService projectService;







    @GetMapping
    public ResponseEntity<ProjectDTO> loadProjectTree(@PathVariable(name = "projectname") String projectname,
                                                      @PathVariable(name = "username") String username){



        return ResponseEntity.ok(projectService.loadProjectByUsernameAndName(username, projectname));

    }

    //
    @PostMapping("/createDirectory")
    public ResponseEntity<Void> createDirectory(@PathVariable(name = "projectname") String projectname,
                                                @PathVariable(name = "username") String username,
                                                @RequestBody DirectoryCreationRequest directoryCreationRequest)
                                                {

        projectService.createDirectory(username, projectname, directoryCreationRequest);

        return ResponseEntity.noContent().build();
    }



    @PostMapping("/createFile")
    public ResponseEntity<Void> createFile(
            @PathVariable(name = "projectname") String projectname,
            @PathVariable(name = "username") String username,
            @RequestBody FileCreationRequest fileCreationRequest) throws Exception {

        projectService.createFile(username, projectname, fileCreationRequest);
        return ResponseEntity.noContent().build();
    }


    // мы не можем удалить корневую папку! поэтому извлечение id происходит по однозначному алгоритму

    @PostMapping("/removeDirectory")
    public ResponseEntity<Void> removeDirectory(
            @PathVariable(name = "projectname") String projectName,
            @PathVariable(name = "username") String username,
            @RequestBody DirectoryRemovalRequest request
            ){

        projectService.deleteDirectory(username, projectName,request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/removeFile")
    public ResponseEntity<Void> removeFile(
            @PathVariable(name = "projectname") String projectName,
            @PathVariable(name = "username") String username,
            @RequestBody FileRemovalRequest removalRequest
            ) throws Exception {


        projectService.deleteFile(username, projectName, removalRequest);

        return ResponseEntity.noContent().build();
    }


}
