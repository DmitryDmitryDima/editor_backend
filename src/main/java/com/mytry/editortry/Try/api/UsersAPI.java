package com.mytry.editortry.Try.api;


import com.mytry.editortry.Try.dto.users.UserDTO;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/")
public class UsersAPI {

    @Autowired
    private UserService userService;


    @GetMapping("/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable("username") String username){

        User user = userService.getUserByUsername(username);



        return ResponseEntity.ok(mapUser(user));

    }


    private UserDTO mapUser(User user){

        UserDTO answer = new UserDTO();
        answer.setUsername(user.getUsername());

        answer.setProjects(user.getProjects().stream().map(Project::getName).toList());
        return answer;
    }


}
