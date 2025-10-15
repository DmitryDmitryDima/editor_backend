package com.mytry.editortry.Try.api;


import com.mytry.editortry.Try.dto.projects.ProjectCreationRequest;
import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.dto.projects.ProjectDeletionRequest;
import com.mytry.editortry.Try.dto.users.UserDTO;
import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.service.ProjectService;
import com.mytry.editortry.Try.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{username}")
public class UsersAPI {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;


    @GetMapping
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable("username") String username){

        User user = userService.getUserByUsername(username);

        user.getProjects().forEach(el->{
            System.out.println(el.getId());
        });



        return ResponseEntity.ok(mapUser(user));

    }



    @PostMapping("/createProject/java")
    public ResponseEntity<Void> createProject(@PathVariable("username") String username,
                                              @RequestBody ProjectCreationRequest projectCreationRequest) throws Exception {

        projectService.createProject(username, projectCreationRequest);
        return ResponseEntity.noContent().build();
    }




    @PostMapping("/deleteProject/java")
    public ResponseEntity<Void> deleteProject(@RequestBody ProjectDeletionRequest projectDeletionRequest){
        projectService.deleteProject(projectDeletionRequest);
        return ResponseEntity.noContent().build();
    }












    private UserDTO mapUser(User user){

        UserDTO answer = new UserDTO();
        answer.setUsername(user.getUsername());

        List<ProjectDTO> projectDTOList = user.getProjects().stream().map(project -> {
            ProjectDTO projectDTO = new ProjectDTO();
            projectDTO.setId(project.getId());
            projectDTO.setName(project.getName());
            return projectDTO;
        }).toList();


        answer.setProjects(projectDTOList);
        return answer;
    }


}
