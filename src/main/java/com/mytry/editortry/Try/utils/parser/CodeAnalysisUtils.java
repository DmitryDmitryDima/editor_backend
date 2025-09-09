package com.mytry.editortry.Try.utils.parser;


import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.mytry.editortry.Try.dto.basicsuggestion.BasicSuggestionContextBasedInfo;
import com.mytry.editortry.Try.dto.basicsuggestion.BasicSuggestionType;
import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionRequest;
import com.mytry.editortry.Try.dto.basicsuggestion.ProjectCacheDTO;
import com.mytry.editortry.Try.dto.dotsuggestion.EditorDotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.EditorDotSuggestionRequest;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectFile;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectType;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionOuterProjectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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





    public static void analyzeExpressionAsInnerType(Expression expression,
                                                    EditorDotSuggestionAnswer answer) throws Exception{

        // пытаемся определить тип. Если он импортирован - вылетит исключение
        ResolvedType resolvedType = expression.calculateResolvedType();

        boolean isJava = resolvedType.describe().startsWith("java.");




        // отдельная обработка массива
        if (resolvedType.isArray()){
            List<String> methods = List.of("clone");
            List<String> fields = List.of("length");
            answer.setMethods(methods);
            answer.setFields(fields);
        }
        // ссылочный тип
        else if (!resolvedType.isPrimitive()){
            ResolvedReferenceType referenceType = resolvedType.asReferenceType();
            // излекаем default и public методы
            List<String> methods = referenceType.getAllMethods()
                    .stream().filter(method ->

                            {
                                String accessSpec = method.accessSpecifier().asString();

                                return accessSpec.equals("public") || !isJava;
                            }

                            )
                    .map(ResolvedDeclaration::getName).distinct().toList();


            List<String> fields = referenceType.getDeclaredFields()
                    .stream().filter(field->

                    {
                        String accessSpec = field.accessSpecifier().asString();
                        return accessSpec.equals("public") || !isJava;
                    })

                    .map(ResolvedDeclaration::getName).distinct().toList();



            answer.setMethods(methods);
            answer.setFields(fields);
        }


    }


    // извлекаем выражение, в чей диапазон входит позиция курсора
    public static Optional<FieldAccessExpr> getExpression(CompilationUnit astTree, EditorDotSuggestionRequest request){
        Position pos = new Position(request.getLine(), request.getColumn());

        return astTree.findFirst(FieldAccessExpr.class, node ->
                node.getRange()
                        .filter(range -> range.contains(pos))
                        .isPresent()
        );

    }




    public static List<BasicSuggestionType> convertCacheAnswerToBasicSuggestionType(List<CacheSuggestionOuterProjectType> types){
        List<BasicSuggestionType> answer = new ArrayList<>();
        types.forEach(chosenType->{
            BasicSuggestionType basicSuggestionType = new BasicSuggestionType();
            basicSuggestionType.setName(chosenType.getName());
            basicSuggestionType.setPackageWay(chosenType.getPackageWay());
            answer.add(basicSuggestionType);
        });
        return answer;
    }


    // из кеша проекта извлекаем подходящие по имени типы
    public static List<BasicSuggestionType> extractTypesFromInnerProjectCache(ProjectCacheDTO state,
                                                                              String userPackage,
                                                                              String typedString){
        List<BasicSuggestionType> types = new ArrayList<>();
        Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation = state.getPackageToFileAssociation();
        for (Map.Entry<String, List<CacheSuggestionInnerProjectFile>> entry:packageToFileAssociation.entrySet()){

            List<CacheSuggestionInnerProjectFile> files = entry.getValue();


            for (var f:files){
                if (f.getPublicType().getName().startsWith(typedString)){
                    BasicSuggestionType basicSuggestionType = new BasicSuggestionType();
                    // формируем импорт только в случае, если не совпадает package
                    if (!userPackage.equals(f.getPackageWay())){
                        basicSuggestionType.setPackageWay(f.getPackageWay());
                    }

                    basicSuggestionType.setName(f.getPublicType().getName());
                    types.add(basicSuggestionType);
                }
            }
        }
        return types;
    }



    // извлекаем package информацию
    public static String extractPackage(CompilationUnit astTree){
        return (astTree.getPackageDeclaration()
                .orElseThrow(()-> new IllegalArgumentException("package detection error")))
                .getNameAsString();
    }


    // публичный api - тут только публичные методы и типы
    public static CacheSuggestionOuterProjectType generateOuterFileApi(CompilationUnit astTree) {
        CacheSuggestionOuterProjectType type = new CacheSuggestionOuterProjectType();


        String packageDeclaration = (astTree.getPackageDeclaration().orElseThrow(() -> new IllegalArgumentException("no package")))
                .getNameAsString();
        type.setPackageWay(packageDeclaration);

        astTree.getTypes().forEach(typeDeclaration -> {
            if (typeDeclaration.isPublic()) {
                type.setName(typeDeclaration.getNameAsString());

                typeDeclaration.getMethods().forEach(method -> {
                    if (method.isPublic()) {
                        type.getMethods().add(method.getNameAsString());
                    }
                });

                typeDeclaration.getFields().forEach(fieldDeclaration -> {
                    if (fieldDeclaration.isStatic() && fieldDeclaration.isPublic()) {
                        fieldDeclaration.findAll(VariableDeclarator.class).forEach(variableDeclarator -> {
                                    type.getFields().add(variableDeclarator.getNameAsString());

                                }
                        );
                    }
                });
            }
        });
        return type;
    }


    // формируем api для внутреннего файла проекта
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

        BlockStmt body = null;

        // если есть тело, анализируем тело
        if(callableDeclaration instanceof NodeWithBlockStmt<?> nodeWithBlock){

           body = nodeWithBlock.getBody();

        }
        else if (callableDeclaration instanceof NodeWithOptionalBlockStmt<?> nodeWithOptionalBlock){
            body = nodeWithOptionalBlock.getBody()
                    .orElseThrow(()->new IllegalArgumentException("no body presents inside method"));
        }

        if (body==null) return variableNames;

        body.findAll(VariableDeclarator.class).forEach(variableDeclarator -> {

            if (variableDeclarator.getNameAsString().startsWith(request.getText())){
                Range range = variableDeclarator.getRange()
                        .orElseThrow(() -> new IllegalArgumentException("invalid variable range"));
                if (range.end.line <= request.getLine()){
                    variableNames.add(variableDeclarator.getNameAsString());
                }

            }
        });
        return variableNames;
    }

    public static List<String> extractImports(CompilationUnit astTree){
        return astTree.getImports().stream().map(ImportDeclaration::getNameAsString).toList();
    }





    // делаем код пригодным для javaparser - в случае с вводом какого-либо набора символом мы комментируем строку
    public static String prepareCode(EditorBasicSuggestionRequest request){
        int lineStart = request.getLineStart();
        String code = request.getCode();

        return code.substring(0, lineStart)+"//"+code.substring(lineStart+1);
    }


    // делаем код пригодным для javaparser - в случае с точкой мы добавляем .dummy поле
    public static String prepareCode(EditorDotSuggestionRequest request){
        return request.getCode().substring(0, request.getPosition())
                +"dummy;"+request.getCode().substring(request.getPosition()+1);
    }


}
