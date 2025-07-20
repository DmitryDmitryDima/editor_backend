package com.mytry.editortry.Try.service.parser;


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
    private final ParserUtils parserUtils;

    public ParserService(){
        parserUtils = new ParserUtils();
    }


    // сценарий поставленной точки

    public DotSuggestionAnswer dotSuggestion(DotSuggestionRequest request){


        return parserUtils.dotParsing(request);


    }

    // запрос импортов
    public ImportAnswer importSuggestion(ImportRequest request){
        return parserUtils.importParsing(request);
    }










}



