package com.mytry.editortry.Try.utils.parser;



import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;

import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import com.mytry.editortry.Try.dto.importsuggestion.ImportAnswer;
import com.mytry.editortry.Try.dto.importsuggestion.ImportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class ParserUtils {

    private static final Logger logger = LoggerFactory.getLogger(ParserUtils.class);




    public ImportAnswer importParsing(ImportRequest importRequest){
        prepareParserConfigForDotSuggestion();

        try {
            HashSet<String> imports = new HashSet<>();
            CompilationUnit c = StaticJavaParser.parse(importRequest.getCode());
            // тестируем возможности определения типов, не импортированных ранее
            // суть алгоритма заключается в том, что мы проверяем существование класса с помощью рефлексии
            // это работает для внутренних java классов, для классов, внешних по отношению к проекту, мы должны загрузить свой resolver
            List<String> languagePackages = List.of(

                    "java.util", "java.io", "java.lang.reflect",
                    "java.time", "java.math", "java.nio"
            );

            c.findAll(ClassOrInterfaceType.class).forEach(el->{
                try {
                    System.out.println(el.getNameAsString() +" name candidate for import");
                    el.resolve().describe();
                }
                catch (Exception e){
                    //e.printStackTrace();
                    for (String p:languagePackages){
                        String fullName = p+"."+el.getNameAsString();

                        try {

                            String importStatement  = "import " +Class.forName(fullName).getPackageName()+"."+el.getNameAsString()+";";
                            System.out.println(importStatement);
                            imports.add(importStatement);
                            break;

                        } catch (ClassNotFoundException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            });

            return new ImportAnswer(imports);
        }
        catch (Exception e){
            e.printStackTrace();
            // пустой ответ
            return new ImportAnswer(new HashSet<>());
        }
    }


    // вставляем слово заглушки
    private String makeCodeComplete(DotSuggestionRequest request){

        return request.getCode().substring(0, request.getPosition())
                +"dummy;"+request.getCode().substring(request.getPosition()+1);

    }


    public DotSuggestionAnswer dotParsing(DotSuggestionRequest request) {






        DotSuggestionAnswer dotSuggestionAnswer = new DotSuggestionAnswer();



        try {

            String s = makeCodeComplete(request);
            prepareParserConfigForDotSuggestion();
            // парсим
            CompilationUnit c = StaticJavaParser.parse(s);









            // извлекаем выражения, соответствующие позиции user
            List<FieldAccessExpr> fieldAccessExprs = c.findAll(FieldAccessExpr.class)
                    .stream().filter(exp->{
                        if (exp.getRange().isEmpty()) return false;
                        return exp.getRange().get().begin.line==request.getLine();
                    }).toList();
            // если таким выражения есть, пытаемся извлечь тип
            if (!fieldAccessExprs.isEmpty()){
                FieldAccessExpr fe = fieldAccessExprs.get(0);

                Expression e = fe.getScope();

                ResolvedType resolvedType = e.calculateResolvedType();

                // отдельная обработка массива
                if (resolvedType.isArray()){
                    List<String> methods = List.of("clone");
                    List<String> fields = List.of("length");
                    dotSuggestionAnswer.setMethods(methods);
                    dotSuggestionAnswer.setFields(fields);

                    return dotSuggestionAnswer;
                }



                List<String> methods = e.calculateResolvedType().asReferenceType().getAllMethods()
                            .stream().filter(m ->
                                    m.accessSpecifier().asString().equals("public")
                                            ||
                                            m.accessSpecifier().asString().isEmpty())
                            .map(ResolvedDeclaration::getName).distinct().toList();


                List<String> fields = e.calculateResolvedType().asReferenceType().getDeclaredFields()
                        .stream().filter(f->
                    f.accessSpecifier().asString().equals("public")).map(ResolvedDeclaration::getName).distinct().toList();



                dotSuggestionAnswer.setMethods(methods);
                dotSuggestionAnswer.setFields(fields);

                return dotSuggestionAnswer;


            }

            else {
                // пустой json
                dotSuggestionAnswer.setFields(new ArrayList<>());
                dotSuggestionAnswer.setMethods(new ArrayList<>());
                return dotSuggestionAnswer;
            }

        }
        catch (Exception e){
            e.printStackTrace();
            dotSuggestionAnswer.setFields(new ArrayList<>());
            dotSuggestionAnswer.setMethods(new ArrayList<>());
            return dotSuggestionAnswer;
        }



















    }
























    // конфигурируем парсер (пока не ясно, можно ли сделать эо один раз)
    private void prepareParserConfigForDotSuggestion(){
        // Конфигурация парсера
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);


    }





}
