package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.projects.DirectoryDTO;
import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import com.mytry.editortry.Try.dto.projects.ProjectDTO;
import com.mytry.editortry.Try.exceptions.project.ProjectNotFoundException;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {


    @Autowired
    private ProjectRepository projectRepository;


    @Transactional
    public void deleteProject(Long id){
        if (!projectRepository.existsById(id)){
            throw new EntityNotFoundException("no found");
        }
        projectRepository.deleteById(id);
    }




    // имя проекта - уникально в пределах одного пользователя
    public ProjectDTO loadProjectByUsernameAndName(String username, String name){

        return mapTree(projectRepository.findByOwnerUsernameAndName(username, name).orElseThrow(ProjectNotFoundException::new));
    }





    // проходим по директория deep-first-traversal, готовя dto

    private void traverse(Directory directory, DirectoryDTO dto,
                          ArrayList<DirectoryDTO> layer,
                          Map<String, FlatTreeMember> flatTree){

        dto.setName(directory.getName());
        dto.setId(directory.getId());

        FlatTreeMember directoryMember = new FlatTreeMember();
        directoryMember.setIndex("directory_"+directory.getId());
        directoryMember.setData(directory.getName());
        directoryMember.setFolder(true);
        directoryMember.setCanMove(true);
        directoryMember.setCanRename(true);



        if (layer!=null){
            layer.add(dto);

        }

        if (layer==null){
            // если корень
            directoryMember.setIndex("root");
            directoryMember.setCanMove(false);
            directoryMember.setCanRename(false);

        }

        /*
        обработка файлов
         */

        if (directory.getFiles()!=null){
            for (File file:directory.getFiles()){
                String index = "file_"+file.getId();
                FlatTreeMember fileMember = new FlatTreeMember();
                fileMember.setIndex(index);
                fileMember.setData(file.getName()+"."+file.getExtension());
                fileMember.setFolder(false);
                fileMember.setCanMove(true);
                fileMember.setCanRename(true);

                directoryMember.getChildren().add(index);

                flatTree.put(index, fileMember);



            }
        }



        if (directory.getChildren().isEmpty()) {
            dto.setChildren(new ArrayList<>());
            //System.out.println("end reached");
        }

        else {
            ArrayList<DirectoryDTO> children = new ArrayList<>();
            dto.setChildren(children);

            for (Directory d:directory.getChildren()){
                String index = "directory_"+d.getId();
                traverse(d, new DirectoryDTO(), children, flatTree);
                directoryMember.getChildren().add(index);
            }
        }

        flatTree.put(directoryMember.getIndex(), directoryMember);
    }


    private ProjectDTO mapTree(Project project){

        //System.out.println(project.getRoot().getChildren()+" children"); // пустой список
        //System.out.println(project.getRoot().getParent()+" parent"); // null


        Directory root = project.getRoot();

        DirectoryDTO rootDTO = new DirectoryDTO();

        Map<String, FlatTreeMember> flatTree = new HashMap<>(); // плоская структура


        traverse(root, rootDTO, null, flatTree);





        ProjectDTO projectDTO = new ProjectDTO();

        projectDTO.setRoot(rootDTO);
        projectDTO.setFlatTree(flatTree);













        return projectDTO;





    }





}
