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

        // в данном случае наверно стоит готовить дерево внутри сервиса, будем смотреть на тяжесть кода
        Project project = projectService.loadProjectByUsernameAndName(username, projectname);


        return ResponseEntity.ok(mapToDTO(project));

    }


    private ProjectDTO mapToDTO(Project project){
        ProjectDTO answer = new ProjectDTO();
        answer.setName(project.getName());
        answer.setId(project.getId());
        return answer;
    }
}
