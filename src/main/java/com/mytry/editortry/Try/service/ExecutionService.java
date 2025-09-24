package com.mytry.editortry.Try.service;

import com.mytry.editortry.Try.dto.execution.EntryPointSetRequest;
import com.mytry.editortry.Try.dto.projects.FileSearchInsideProjectResult;
import com.mytry.editortry.Try.dto.run.ProjectRunRequest;
import com.mytry.editortry.Try.dto.run.ProjectStopRequest;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.ProjectRepository;
import com.mytry.editortry.Try.utils.ProjectUtils;
import com.mytry.editortry.Try.utils.processes.ExecutionProcessWithCallback;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessCreationEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessErrorEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessInterruptionEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessMessageEvent;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExecutionService  {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private FileRepository fileRepository;



    @Value("${files.directory}")
    private String disk_location;

    @Autowired
    private ApplicationEventPublisher publisher;


    // проставляя entry point, мы должны внести изменения в pom.xlm, для этого нам необходимо сформировать путь к файлу
    @Transactional
    public void setEntryPoint(EntryPointSetRequest request) throws Exception {

        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(()->new IllegalArgumentException("no such project exists"));

        // проверяем, не является файл уже точкой входа
        if (project.getEntryPoint()!=null && project.getEntryPoint().getId().equals(request.getFileId())){
            throw new IllegalArgumentException("this file is already entry point");
        }

        // попадаем в директорию с файлами проекта
        Directory javaRoot = ProjectUtils.getMavenClassicalStructureRoot(project.getRoot());


        // проверяем, что файл принадлежит проекту с заданным айди
        Optional<FileSearchInsideProjectResult> fileInfo = ProjectUtils
                .findFileInsideProjectWithTrace(javaRoot, request.getFileId());

        if (fileInfo.isEmpty()){
            throw new IllegalArgumentException("this file is not a part of project");
        }
        // только java файл может быть исполняемым
        if (!fileInfo.get().getFile().getExtension().equals("java")){
            throw new IllegalArgumentException("this file has not valid extension");
        }

        // создаем зависимость внутри базы данных
        File entryPoint = fileInfo.get().getFile();
        project.setEntryPoint(entryPoint);

        // формируем внутренний адрес файла (package)
        String filePath = ProjectUtils.createPathFromAccumulatedCollection(fileInfo.get().getPath());
        filePath = filePath.replace("/", ".")+entryPoint.getName();
        // формируем путь к pom.xml
        String username = project.getOwner().getUsername();
        String projectname = project.getName();
        String pomPath = disk_location+username+"/projects/"+projectname+"/pom.xml";
        // получив данные, редактируем pom.xml
        ProjectUtils.setMainClassInsidePomXML(pomPath, filePath);





    }


    public void runProject(ProjectRunRequest request){


        // готовим процесс со всей нужной информацией, в том числе с нужными для процесса коллбеками
        ExecutionProcessWithCallback preparedProcess = new ExecutionProcessWithCallback(
                (ExecutionProcessInterruptionEvent evt)->publisher.publishEvent(evt),
                (ExecutionProcessMessageEvent evt)->publisher.publishEvent(evt),
                (ExecutionProcessErrorEvent evt)->publisher.publishEvent(evt),
                request.getProjectId()
        );

        // публикуем ивент создания процесса, передавая в нем сам процесс. Ивент ловится в ProcessStateManager
        ExecutionProcessCreationEvent creationEvent = new ExecutionProcessCreationEvent(this, preparedProcess);
        publisher.publishEvent(creationEvent);







    }


    public void stopProject(ProjectStopRequest request){

        ExecutionProcessInterruptionEvent interruptionEvent = new ExecutionProcessInterruptionEvent(this,
                request.getProjectId(),
                ExecutionProcessInterruptionEvent.InterruptionType.External);

        publisher.publishEvent(interruptionEvent);





    }


}
