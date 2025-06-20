package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{username}/")
public class ProjectsAPI {

    @Autowired
    private ProjectService projectService;


    @GetMapping("/{projectname}")
    public ResponseEntity<ProjectDTO> loadProjectTree(@PathVariable(name = "projectname") String projectname,
                                                      @PathVariable(name = "username") String username){



        return ResponseEntity.ok(projectService.loadProjectByUsernameAndName(username, projectname));

    }



}
