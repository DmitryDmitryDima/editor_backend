package com.mytry.editortry.Try.api;


import com.mytry.editortry.Try.dto.execution.EntryPointSetRequest;
import com.mytry.editortry.Try.dto.run.ProjectRunRequest;
import com.mytry.editortry.Try.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tools/execution/java/")
public class ExecutionAPI {

    @Autowired
    private ExecutionService executionService;


    /*
    помечаем файл как главный

     */
    @PostMapping("/setEntryPoint")
    public void setEntryPoint(@RequestBody EntryPointSetRequest request){
        executionService.setEntryPoint(request);
    }

    /*
    запускаем проект - метод может быть вызван из разных точек приложения
     */
    @PostMapping("/run")
    public void run(@RequestBody ProjectRunRequest request){
        executionService.runProject(request);
    }




}
