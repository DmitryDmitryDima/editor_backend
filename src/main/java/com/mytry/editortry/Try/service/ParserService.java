package com.mytry.editortry.Try.service;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import com.mytry.editortry.Try.utils.parser.ParserUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Service
public class ParserService {

    // код, относящийся к анализу и парсингу
    private final ParserUtils parserUtils;

    public ParserService(){
        parserUtils = new ParserUtils();
    }

    public DotSuggestionAnswer dotSuggestion(DotSuggestionRequest request){


        try {
            return parseAndSuggest(request);
        }
        catch (Exception e){
            e.printStackTrace();
            // возвращаем пустой json
            return new DotSuggestionAnswer(new ArrayList<>(), new ArrayList<>());
        }


    }

    // todo для этого метода можно использовать overloading - выполняемый метод будет определяться в зависимости от вида запроса
    private DotSuggestionAnswer parseAndSuggest(DotSuggestionRequest request) throws Exception {

        return parserUtils.dotParsing(request);
    }






}



