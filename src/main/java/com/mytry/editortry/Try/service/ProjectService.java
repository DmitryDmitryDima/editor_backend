package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.exceptions.project.ProjectNotFoundException;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {


    @Autowired
    private ProjectRepository projectRepository;


    // имя проекта - уникально в пределах одного пользователя
    public Project loadProjectByUsernameAndName(String username, String name){
        return projectRepository.findByOwnerUsernameAndName(username, name).orElseThrow(ProjectNotFoundException::new);
    }





}
