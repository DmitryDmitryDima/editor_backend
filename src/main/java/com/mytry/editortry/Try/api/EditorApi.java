package com.mytry.editortry.Try.api;


import com.mytry.editortry.Try.dto.*;
import com.mytry.editortry.Try.service.CompilerService;
import com.mytry.editortry.Try.service.LanguageModelService;
import com.mytry.editortry.Try.service.ParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/editor/")
public class EditorApi {

    @Autowired
    private CompilerService compilerService;

    @Autowired
    private ParserService parserService;

    @Autowired
    private LanguageModelService languageModelService;



    // компиляция
    @PostMapping("/run/")
    public RunAnswer run(@RequestBody RunRequest request) throws Exception {


        String info = request.getInfo();


        System.out.println(languageModelService.sendARequest(LanguageModelService.IMPORT_PROMPT + info).importOptimize());


        return new RunAnswer(compilerService.makeCompilation(info));
    }




    // предложки
    @PostMapping("/dotSuggest/")
    public DotSuggestionAnswer dot(@RequestBody DotSuggestionRequest request ) {

        // Я внес некоторое изменение тут и сделал коммит в мастер ветку

        return new DotSuggestionAnswer(parserService.methodSuggestions(request));
    }



    // предложка импорта - обращение к нейросети (gemma3 or qwen coder)
    // т.к. нейросеть нестабильна в своих ответах, необходимо реализовать возможность делать ctrl Z
    @PostMapping("/import/")
    public ImportAnswer suggestImport(@RequestBody Map<String, String> request){

        String code = request.get("code");


        Set<String> importAnswer = languageModelService
                .sendARequest(LanguageModelService.IMPORT_PROMPT+code).importOptimize();

        return new ImportAnswer(importAnswer);
    }




}
