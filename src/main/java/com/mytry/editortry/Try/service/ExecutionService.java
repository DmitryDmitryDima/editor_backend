package com.mytry.editortry.Try.service;

import com.mytry.editortry.Try.dto.execution.EntryPointSetRequest;
import com.mytry.editortry.Try.dto.run.ProjectRunRequest;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.utils.execution.ExecutionServiceUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExecutionService {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private FileRepository fileRepository;


    // проставляя entry point, мы должны внести изменения в pom.xlm, для этого нам необходимо сформировать путь к файлу
    @Transactional
    public void setEntryPoint(EntryPointSetRequest request){

        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(()->new IllegalArgumentException("no such project exists"));

        // проверяем, не является файл уже точкой входа
        if (project.getEntryPoint()!=null && project.getEntryPoint().getId().equals(request.getFileId())){
            throw new IllegalArgumentException("this file is already entry point");
        }

        // проверяем, что файл принадлежит проекту с заданным айди
        Optional<File> file = ExecutionServiceUtils
                .findFileInsideProject(project.getRoot(), request.getFileId());

        if (file.isEmpty()){
            throw new IllegalArgumentException("this file is not a part of project");
        }

        File entryPoint = file.get();
        project.setEntryPoint(entryPoint);



    }

    @Transactional
    public void runProject(ProjectRunRequest request){
        // проверяем, существует ли проект
        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(()->new IllegalArgumentException("no such project exists"));
        // проверяем, существует ли точка входа в проект
        File entryPoint = project.getEntryPoint();
        if (entryPoint==null){
            throw new IllegalStateException("no entry point");
        }

        // блокируем проект

    }


}
