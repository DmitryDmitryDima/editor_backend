package com.mytry.editortry.Try.service;

import com.mytry.editortry.Try.dto.execution.EntryPointSetRequest;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ExecutionService {

    private ProjectRepository projectRepository;
    private FileRepository fileRepository;


    @Transactional
    public void setEntryPoint(EntryPointSetRequest request){

        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(()->new IllegalArgumentException("no such project exists"));

        // проверяем, что файл принадлежит проекту с заданным айди
    }


}
