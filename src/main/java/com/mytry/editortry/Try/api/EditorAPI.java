package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.files.EditorFileReadAnswer;
import com.mytry.editortry.Try.dto.files.EditorFileReadRequest;
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


    @PostMapping(value = "/readAndCache")
    public ResponseEntity<EditorFileReadAnswer> readAndCacheFile(
            @RequestBody EditorFileReadRequest request
            ){


        return ResponseEntity.ok(editorService.loadFile(request));

    }

}
