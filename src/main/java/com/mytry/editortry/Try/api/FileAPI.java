package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.files.TextFileDTO;
import com.mytry.editortry.Try.service.FilesService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Контроллер, отвечающий непосредственно за редактор - предложка + связанные с файлом операции

@RestController
@RequestMapping("/api/users/{username}/projects/{projectname}/**")
public class FileAPI {

    @Autowired
    private FilesService filesService;


    // при первом fetch создается запись в кэш
    @GetMapping
    public ResponseEntity<TextFileDTO> fetchFileData(
            @PathVariable("username") String username,
            @PathVariable("projectname") String projectname, HttpServletRequest request

    ) {

        String url = request.getRequestURL().toString();
        String filepath = url.split(projectname+"/")[1];




        return ResponseEntity.ok(filesService.loadFile(username,projectname,filepath));
    }






}
