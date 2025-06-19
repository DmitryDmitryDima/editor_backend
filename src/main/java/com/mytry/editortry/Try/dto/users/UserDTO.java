package com.mytry.editortry.Try.dto.users;

import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.model.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private String username;

    private List<ProjectDTO> projects;



}
