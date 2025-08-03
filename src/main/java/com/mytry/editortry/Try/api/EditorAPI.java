package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.files.EditorFileReadAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileReadRequest;
import com.mytry.editortry.Try.dto.files.EditorFileSaveAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileSaveRequest;
import com.mytry.editortry.Try.service.EditorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tools/editor")
public class EditorAPI {

    @Autowired
    private EditorService editorService;

    // читаем файл
    @PostMapping(value = "/load")
    public ResponseEntity<EditorFileReadAnswer> readAndCacheFile(
            @RequestBody EditorFileReadRequest request
            ){


        return ResponseEntity.ok(editorService.loadFile(request));

    }



    // сохраняем файл, пишем информацию в кеш, уведомляем все сессии, причастные к проекту
    @PostMapping("/save")
    public ResponseEntity<EditorFileSaveAnswer> save(@RequestBody EditorFileSaveRequest request){

        System.out.println(request.getClientTime());
        return ResponseEntity.ok(editorService.saveFile(request));
    }

}
