package com.mytry.editortry.Try.utils.processes;


import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProjectDatabaseLock {

    @Autowired
    private ProjectRepository projectRepository;



    public void unlock(Long projectId) throws Exception{
        Project project = projectRepository.findById(projectId).orElseThrow(()->
                new IllegalStateException("process event trying to access non existent project")
        );

        // меняем флаг
        project.setRunning(false);
    }


    public void checkIfStopped(Long projectId) throws Exception{


        Project project = projectRepository.findById(projectId).orElseThrow(()->
                new IllegalStateException("process event trying to access non existent project")
        );


        if (!project.isRunning()){
            throw new IllegalStateException("process already stopped");
        }
    }

    public String lockProjectAndGenerateDiskPath(Long projectId) throws Exception{
        Project project = projectRepository.findById(projectId).orElseThrow(()->
                new IllegalStateException("process event trying to access non existent project")
        );

        // проверяем, существует ли точка входа в проект
        File entryPoint = project.getEntryPoint();
        if (entryPoint==null){
            throw new IllegalStateException("no entry point");
        }

        // проверяем, не запущен ли проект уже
        if (project.isRunning()){
            throw new IllegalStateException("project is already running");
        }

        // блокируем
        project.setRunning(true);

        return project.getOwner().getUsername()+"/projects/"+project.getName()+"/";

    }


}
