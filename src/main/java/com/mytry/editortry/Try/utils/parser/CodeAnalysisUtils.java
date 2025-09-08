package com.mytry.editortry.Try.utils.parser;


import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.mytry.editortry.Try.dto.basicsuggestion.BasicSuggestionContextBasedInfo;
import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionRequest;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectFile;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// утилитарный класс для анализа готовой ast tree
public class CodeAnalysisUtils {



    /*методы класса object*/
    public final static List<String> objectMethods = List
            .of("wait", "getClass", "hashCode","toString","clone", "equals", "notify", "notifyAll");

    /*ключевые слова, доступные в методе*/
    public final static List<String> availableKeywordsForMethodAndConstructor = List.of("while","enum",
            "record","this", "try", "throw", "if", "int",
            "interface", "short", "super", "switch",
            "synchronized","do", "double", "final",
            "for", "float",
            "long", "class", "char","var","boolean", "byte","new"
    );

    /*ключевые слова, доступные вне типа*/
    public final static List<String> availableKeywordsOutsideOfType =  List.of("enum", "record","import",
            "interface",
            "public","abstract","sealed","final","class","non-sealed");


    public final static List<String> mavenFolderStructure = List.of("src", "main","java","com");









    // извлекаем package информацию
    public static String extractPackage(CompilationUnit astTree){
        return (astTree.getPackageDeclaration()
                .orElseThrow(()-> new IllegalArgumentException("package detection error")))
                .getNameAsString();
    }

    public static CacheSuggestionInnerProjectFile generateInnerProjectFileAPI(CompilationUnit astTree){
        CacheSuggestionInnerProjectFile file = new CacheSuggestionInnerProjectFile();
        String packageDeclaration = extractPackage(astTree);

        file.setPackageWay(packageDeclaration);

        astTree.getTypes().forEach(type->{

            CacheSuggestionInnerProjectType cacheType = new CacheSuggestionInnerProjectType();
            cacheType.setName(type.getNameAsString());
            // определяем access modifier для типа
            if (type.isPublic()){
                file.setPublicType(cacheType);
            }
            else if (!type.isPrivate() && !type.isNestedType() && !type.isProtected()){
                file.getDefaultTypes().add(cacheType);
            }
            // анализируем методы - разделяем статичные и нестатичные методы для удобства чтения кеша
            type.getMethods().forEach(method->{
                String name = method.getNameAsString();
                if (method.isStatic()){
                    if (method.isPublic()){
                        cacheType.getPublicStaticMethods().add(name);
                    }
                    else if (method.isDefault()){
                        cacheType.getDefaultStaticMethods().add(name);
                    }
                }
                else {
                    if (method.isPublic()){
                        cacheType.getPublicMethods().add(name);
                    }
                    else if (method.isDefault()){
                        cacheType.getDefaultMethods().add(name);
                    }
                }

            });

            // анализируем поля - разделяем статичные и нестатичные поля для удобства чтения кеша
            type.getFields().forEach(fieldDeclaration -> {


                List<String> variables = fieldDeclaration.getVariables().stream().map(NodeWithSimpleName::getNameAsString).toList();

                if (fieldDeclaration.isStatic()){
                    if (fieldDeclaration.isPublic()){
                        cacheType.getPublicStaticFields().addAll(variables);

                    }
                    else if (!fieldDeclaration.isPrivate() && !fieldDeclaration.isProtected()){
                        cacheType.getDefaultStaticFields().addAll(variables);
                    }
                }
                else {
                    if (fieldDeclaration.isPublic()){
                        cacheType.getPublicFields().addAll(variables);

                    }
                    else if (!fieldDeclaration.isPrivate() && !fieldDeclaration.isProtected()){
                        cacheType.getDefaultFields().addAll(variables);
                    }
                }


            });



        });


        return file;
    }

    // собираем типы и вычисляем, внутри какого типа находится пользователь
    public static Optional<TypeDeclaration<?>> collectAndCheckTypes(CompilationUnit astTree,
                                                              BasicSuggestionContextBasedInfo info,
                                                              EditorBasicSuggestionRequest request){
        return astTree.getTypes().stream().filter(
                typeDeclaration -> {
                    if (!typeDeclaration.isNestedType()){
                        info.getTypes().add(typeDeclaration.getNameAsString());
                    }

                    Range r = typeDeclaration.getRange().orElseThrow(()->new IllegalArgumentException("invalid type range"));
                    return r.begin.line<= request.getLine()&&r.end.line>= request.getLine();
                }
        ).findFirst();
    }

    // собираем конструкторы и вычисляем, внутри какого находится пользователь
    public static Optional<ConstructorDeclaration> collectAndCheckConstructors(TypeDeclaration<?> type,
                                                                               EditorBasicSuggestionRequest request){
        return type.getConstructors()
                .stream().filter(constructorDeclaration -> {
                    Range range = constructorDeclaration.getRange()
                            .orElseThrow(()->new IllegalArgumentException("invalid constructor range"));
                    return range.begin.line<= request.getLine()&&range.end.line>= request.getLine();
                }).findFirst();
    }

    // собираем методы и вычисляем, в каком находится юзер
    public static Optional<MethodDeclaration> collectAndCheckMethods(TypeDeclaration<?> type,
                                                                     EditorBasicSuggestionRequest request, List<String> staticMethods,
                                                                     List<String> nonStaticMethods){

        return type.getMethods().stream()
                .peek(methodDeclaration -> {
                    String methodName = methodDeclaration.getNameAsString();
                    // фильтруем по введенным символам
                    if (methodName.startsWith(request.getText())){
                        if (methodDeclaration.isStatic()){
                            staticMethods.add(methodName);
                        }
                        else {
                            nonStaticMethods.add(methodName);
                        }
                    }
                }).filter(methodDeclaration -> {
                    Range r = methodDeclaration.getRange()
                            .orElseThrow(()->new IllegalArgumentException("invalid method range"));
                    return r.begin.line<= request.getLine()&&r.end.line>= request.getLine();
                }).findFirst();
    }

    // собираем и фильтруем поля класса, разделяя их на статичные и нестатичные
    public static void collectAndSplitFields(TypeDeclaration<?> type,
                                            String typedCode,
                                            List<String> staticFields,
                                            List<String> nonStaticFields){

        type.getFields().forEach(fieldDeclaration -> {
            List<String> variables = fieldDeclaration.getVariables().stream().map(NodeWithSimpleName::getNameAsString)
                    .filter(name->name.startsWith(typedCode)).toList();

            if (fieldDeclaration.isStatic()){
                staticFields.addAll(variables);
            }
            else {
                nonStaticFields.addAll(variables);
            }
        });
    }



    // извлекаем локальные переменные из метода или конструктора
    public static List<String> extractLocalVariablesAndParameters(CallableDeclaration<?> callableDeclaration,
                                                            EditorBasicSuggestionRequest request){
        // получаем параметры
        List<String> variableNames = new ArrayList<>(callableDeclaration.getParameters().stream()
                .map(NodeWithSimpleName::getNameAsString).filter(name->name.startsWith(request.getText())).toList());

        // если есть тело, анализируем тело
        if(callableDeclaration instanceof NodeWithBlockStmt<?> nodeWithBlock){
            nodeWithBlock.getBody().findAll(VariableDeclarator.class).forEach(variableDeclarator -> {
                if (variableDeclarator.getNameAsString().startsWith(request.getText())){
                    Range range = variableDeclarator.getRange()
                            .orElseThrow(() -> new IllegalArgumentException("invalid variable range"));
                    if (range.end.line <= request.getLine()){
                        variableNames.add(variableDeclarator.getNameAsString());
                    }

                }
            });

        }
        return variableNames;
    }





    // делаем код пригодным для javaparser - в случае с вводом какого-либо набора символом мы комментируем строку
    public static String prepareCode(EditorBasicSuggestionRequest request){
        int lineStart = request.getLineStart();
        String code = request.getCode();

        return code.substring(0, lineStart)+"//"+code.substring(lineStart+1);
    }


}
