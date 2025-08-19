package com.mytry.editortry.Try.service.codeanalyzis;


import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import com.mytry.editortry.Try.dto.importsuggestion.ImportAnswer;
import com.mytry.editortry.Try.dto.importsuggestion.ImportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ParserService {

    private static final Logger logger = LoggerFactory.getLogger(ParserService.class);


    // код, относящийся к анализу и парсингу
    private final CodeAnalyzer codeAnalyzer;

    public ParserService(){
        codeAnalyzer = new CodeAnalyzer();
    }


    // сценарий поставленной точки

    public DotSuggestionAnswer dotSuggestion(DotSuggestionRequest request){


        return codeAnalyzer.dotParsing(request);


    }

    // запрос импортов
    public ImportAnswer importSuggestion(ImportRequest request){
        return codeAnalyzer.importParsing(request);
    }










}



