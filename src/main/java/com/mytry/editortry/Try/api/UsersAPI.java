package com.mytry.editortry.Try.api;


import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.dto.users.UserDTO;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/")
public class UsersAPI {

    @Autowired
    private UserService userService;


    @GetMapping("/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable("username") String username){

        User user = userService.getUserByUsername(username);

        user.getProjects().forEach(el->{
            System.out.println(el.getId());
        });



        return ResponseEntity.ok(mapUser(user));

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
