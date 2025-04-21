package com.mytry.editortry.Try.service;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mytry.editortry.Try.dto.DotSuggestionRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*
в будущем - реализация кеша


Проблема - java parser нуждается в правильном коде и импорте

Решение 1 - присылать закоменченный код перед объектом, после чего по имени объекта (переменной) искать методы
У этого решения потенциально есть сложность - как быть с ситуацией, когда метод вызывается из метода



 */
@Service
public class ParserService {


    public List<String> methodSuggestions(DotSuggestionRequest request){

        try {
            return parseAndSuggest(request);
        }
        catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }


    }


    private List<String> parseAndSuggest(DotSuggestionRequest request) throws Exception {

        // сначала нужно попытаться отредактировать код так, чтобы он был корректным для парсера
        System.out.println(request.getObject());
        String completedCode = request.makeCodeComplete();


        // Конфигурация парсера - тут мы, как минимум, создаем инструменты распознавания типов

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        CompilationUnit c = StaticJavaParser.parse(completedCode);

        Map<String, List<String>> variablesMap = new HashMap<>();

        VariablesCollector vc = new VariablesCollector(request.getObject());
        vc.visit(c,variablesMap);
        System.out.println(variablesMap); // все переменные






        return variablesMap.get(request.getObject());
    }


    //TODO КАК НАЙТИ КОНТЕКСТ В СЛУЧАЕ СТОЛКНОВЕНИЯ ДВУХ ПЕРЕМЕННЫХ С ОДНИМ ИМЕНЕМ
    //Обработка случая вызова метода из метода (точка после скобки)



    private static class VariablesCollector extends VoidVisitorAdapter<Map<String, List<String>>> {

        private String objName;

        public VariablesCollector(String name){
            this.objName = name;
        }

        @Override
        public void visit(VariableDeclarator variableDeclaration, Map<String, List<String>> map){
            super.visit(variableDeclaration, map);

            if (!variableDeclaration.getName().asString().equals(objName)) return;




            List<String> methods = new ArrayList<>();

            ResolvedType resolvedType = variableDeclaration.getType().resolve();



            // рассматриваем сценарии с массивом и ссылкой
            try {
                if(resolvedType.isArray()){

                    // для массивов можно придумать комбинации со стримами
                    methods.addAll(List.of("length", "clone"));
                }

                else {
                    // извлекаем методы
                    resolvedType.asReferenceType().getAllMethods()
                            .stream().filter(m -> m.accessSpecifier().asString().equals("public") ||
                                    m.accessSpecifier().asString().isEmpty())
                            .map(ResolvedDeclaration::getName).distinct()
                            .forEach(methods::add);

                    // тестируем поля и внутренние классы
                }



            }
            catch (Exception e){
                e.printStackTrace();

            }

            map.put(variableDeclaration.getNameAsString(), methods);

        }
    }
}



