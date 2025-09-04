package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionAnswer;
import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionRequest;
import com.mytry.editortry.Try.dto.dotsuggestion.EditorDotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.EditorDotSuggestionRequest;
import com.mytry.editortry.Try.dto.files.EditorFileReadAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileReadRequest;
import com.mytry.editortry.Try.dto.files.EditorFileSaveAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileSaveRequest;
import com.mytry.editortry.Try.service.EditorServiceOld;
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
    private EditorServiceOld editorServiceOld;

    // читаем файл
    @PostMapping(value = "/load")
    public ResponseEntity<EditorFileReadAnswer> readAndCacheFile(
            @RequestBody EditorFileReadRequest request
            ){


        return ResponseEntity.ok(editorServiceOld.loadFile(request));

    }



    // сохраняем файл, пишем информацию в кеш, уведомляем все сессии, причастные к проекту
    @PostMapping("/save")
    public ResponseEntity<EditorFileSaveAnswer> save(@RequestBody EditorFileSaveRequest request){

        System.out.println(request.getClientTime());
        return ResponseEntity.ok(editorServiceOld.saveFile(request));
    }


    // предложка, вызываемая, когда пользователь пишет что-то кроме точки
    // потенциальные проблемы - огромное число запросов + реализация автоимпорта
    @PostMapping("/completions/basic")
    public ResponseEntity<EditorBasicSuggestionAnswer> basicSuggestion(@RequestBody EditorBasicSuggestionRequest request){

        return ResponseEntity.ok(editorServiceOld.basicSuggestion(request));

    }

    // предложка для точки
    @PostMapping("/completions/dot")
    public ResponseEntity<EditorDotSuggestionAnswer> dotSuggestion(@RequestBody EditorDotSuggestionRequest request){
        return ResponseEntity.ok(editorServiceOld.dotSuggestion(request));
    }

}
