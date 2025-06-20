package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.projects.DirectoryDTO;
import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.exceptions.project.ProjectNotFoundException;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectService {


    @Autowired
    private ProjectRepository projectRepository;




    // имя проекта - уникально в пределах одного пользователя
    public ProjectDTO loadProjectByUsernameAndName(String username, String name){

        return mapTree(projectRepository.findByOwnerUsernameAndName(username, name).orElseThrow(ProjectNotFoundException::new));
    }


    // проходим по директория deep-first-traversal, готовя dto

    private void traverse(Directory directory, DirectoryDTO dto, ArrayList<DirectoryDTO> layer){

        dto.setName(directory.getName());
        dto.setId(directory.getId());

        if (layer!=null){
            layer.add(dto);
        }

        if (directory.getChildren().isEmpty()) {

            System.out.println("end reached");
        }

        else {
            ArrayList<DirectoryDTO> children = new ArrayList<>();
            dto.setChildren(children);

            for (Directory d:directory.getChildren()){
                traverse(d, new DirectoryDTO(), children);
            }
        }
    }


    private ProjectDTO mapTree(Project project){

        //System.out.println(project.getRoot().getChildren()+" children"); // пустой список
        //System.out.println(project.getRoot().getParent()+" parent"); // null


        Directory root = project.getRoot();

        DirectoryDTO rootDTO = new DirectoryDTO();


        traverse(root, rootDTO, null);





        ProjectDTO projectDTO = new ProjectDTO();

        projectDTO.setRoot(rootDTO);













        return projectDTO;





    }





}
