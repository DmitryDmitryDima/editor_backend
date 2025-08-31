package com.mytry.editortry.Try.service.codeanalyzis;



import com.github.javaparser.JavaParser;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;

import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import com.mytry.editortry.Try.dto.basicsuggestion.BasicSuggestionContextBasedInfo;
import com.mytry.editortry.Try.dto.basicsuggestion.ProjectTypesDTO;
import com.mytry.editortry.Try.dto.dotsuggestion.EditorDotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.EditorDotSuggestionRequest;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.utils.cache.*;
import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionAnswer;
import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionRequest;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import com.mytry.editortry.Try.dto.importsuggestion.ImportAnswer;
import com.mytry.editortry.Try.dto.importsuggestion.ImportRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

@Service
public class CodeAnalyzer {



    @Autowired
    private JavaParser parser;

    private static final Logger logger = LoggerFactory.getLogger(CodeAnalyzer.class);



    private final List<String> objectMethods = List
            .of("wait", "getClass", "hashCode","toString","clone", "equals", "notify", "notifyAll");




    public EditorDotSuggestionAnswer dotSuggestion(EditorDotSuggestionRequest request,
                                                   Map<String, List<CacheSuggestionInnerProjectFile>> cache){



        EditorDotSuggestionAnswer dotSuggestionAnswer = new EditorDotSuggestionAnswer();
        String code = makeCodeComplete(request);



        CompilationUnit c = parser.parse(code).getResult().orElseThrow(()->new IllegalArgumentException("parsing failed"));

        // извлекаем импорты, чтобы потом соотнести тип с импортом
        List<String> imports = c.getImports().stream().map(ImportDeclaration::getNameAsString).toList();


        // извлекаем выражения, соответствующие позиции user
        List<FieldAccessExpr> fieldAccessExprs = c.findAll(FieldAccessExpr.class)
                .stream().filter(exp->{
                    if (exp.getRange().isEmpty()) return false;
                    return exp.getRange().get().begin.line==request.getLine();
                }).toList();

        System.out.println(fieldAccessExprs);

        // если таким выражения есть, пытаемся извлечь тип
        if (!fieldAccessExprs.isEmpty()){
            FieldAccessExpr fe = fieldAccessExprs.get(0);

            Expression e = fe.getScope();


            try {
                var resolvedType = e.calculateResolvedType();

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

            }
            catch (UnsolvedSymbolException exception){


                // скорее всего тип относится к импорту
                String[] expressionDetails = e.toString().split("\\.");

                // пока что рассматриваем сценарий одной точки
                String root = expressionDetails[0];
                String rootType = null;

                // сценарий - если это переменная - также это может быть вызов из класса
                for (VariableDeclarator vr:c.findAll(VariableDeclarator.class)){
                    if (vr.getNameAsString().equals(root)){

                        rootType = vr.getTypeAsString();
                    }
                }

                // тип не найден - возвращаем пустой ответ
                if (rootType == null){
                    return dotSuggestionAnswer;
                }
                // ищем совпадающий импорт
                else {
                    String matchingImport = null;
                    for (String i:imports){
                        if (i.endsWith(rootType)){
                            matchingImport = i;
                        }
                    }

                    if (matchingImport == null){
                        return dotSuggestionAnswer;
                    }
                    else {
                        // ищем в кеше
                        String way = matchingImport.substring(0, matchingImport.length()-rootType.length()-1);

                        List<CacheSuggestionInnerProjectFile> files = cache.get(way);
                        for (var f:files){
                            // todo если package совпадает, мы должны смотреть не только публичные методы
                            if (f.getPublicType().getName().equals(rootType)){

                                dotSuggestionAnswer.setMethods(f.getPublicType().getMethods());
                                dotSuggestionAnswer.setFields(f.getPublicType().getFields());
                            }
                        }

                    }
                }





            }



            //ResolvedType resolvedType = e.calculateResolvedType();

        }



        return dotSuggestionAnswer;

    }




    // генерируем публичное api внутреннего файла (учитываем, что может быть несколько типов) - метод используется как в точечном анализе, так и в глобальном
    public CacheSuggestionInnerProjectFile generateFileCache(String code) throws Exception{
        CacheSuggestionInnerProjectFile file = new CacheSuggestionInnerProjectFile();
        CompilationUnit c = parser.parse(code).getResult().orElseThrow(()->new IllegalArgumentException("parsing failed"));

        String packageDeclaration = (c.getPackageDeclaration().orElseThrow(()-> new IllegalArgumentException("no package")))
                .getNameAsString();
        file.setPackageWay(packageDeclaration);

        c.getTypes().forEach(el->{
            if (el.isPublic()){
                CacheSuggestionInnerProjectType cacheSuggestionType = new CacheSuggestionInnerProjectType();
                cacheSuggestionType.setName(el.getNameAsString());
                file.setPublicType(cacheSuggestionType);
            }

        });


        return file;

    }

    // публичный api - тут только публичные методы
    public CacheSuggestionOuterProjectType generateJavaFileOuterApi(String code){
        CacheSuggestionOuterProjectType file = new CacheSuggestionOuterProjectType();
        CompilationUnit c = parser.parse(code).getResult().orElseThrow(()->new IllegalArgumentException("parsing failed"));

        String packageDeclaration = (c.getPackageDeclaration().orElseThrow(()-> new IllegalArgumentException("no package")))
                .getNameAsString();
        file.setPackageWay(packageDeclaration);

        c.getTypes().forEach(el->{
            if (el.isPublic()){
                file.setName(el.getNameAsString());

                el.getMethods().forEach(method->{
                    if (method.isPublic()){
                        file.getMethods().add(method.getNameAsString());
                    }
                });

                el.getFields().forEach(fieldDeclaration -> {
                    if (fieldDeclaration.isStatic() && fieldDeclaration.isPublic()){
                        fieldDeclaration.findAll(VariableDeclarator.class).forEach(v->{
                                    file.getFields().add(v.getNameAsString());

                                }
                        );
                    }
                });
            }
        });



        return file;
    }




    // root path + layer + filename = > full way
    private void collectAndAnalyzeFiles(ArrayList<String> layer,
                                        Directory directory,
                                        ProjectTypesDTO dto) throws Exception{

        layer.add(directory.getName());


        for (File f:directory.getFiles()){
            if (f.getExtension().equals("java")){
                StringBuilder filePath = new StringBuilder();

                for (String s:layer){
                    filePath.append(s);
                    filePath.append("/");
                }
                filePath.append(f.getName()).append(".").append(f.getExtension());



                java.io.File openedFile = new java.io.File(filePath.toString());

                StringBuilder sb = new StringBuilder();

                try (FileReader r = new FileReader(openedFile);
                     BufferedReader bufferedReader = new BufferedReader(r);
                ){

                    String s;
                    while ((s = bufferedReader.readLine())!=null){

                        sb.append(s+"\n");
                    }
                }
                catch (Exception e){
                    throw new IllegalArgumentException("error while reading disk");
                }

                // заполняем dto
                CacheSuggestionInnerProjectFile suggestion = generateFileCache(sb.toString());

                // id ассоциация
                dto.getIdToFileAssociation().put(f.getId(), suggestion);

                // package ассоциация
                Map<String, List<CacheSuggestionInnerProjectFile>> packages = dto.getPackageToFileAssociation();

                var packageList = packages.get(suggestion.getPackageWay());
                if (packageList == null){
                    ArrayList<CacheSuggestionInnerProjectFile> list = new ArrayList<>();
                    list.add(suggestion);
                    packages.put(suggestion.getPackageWay(), list);
                }
                else {
                    packageList.add(suggestion);
                }





            }

        }
        for (Directory ch:directory.getChildren()){
            collectAndAnalyzeFiles(layer, ch, dto);
        }

        // удаляем пройденную директорию из пути
        layer.remove(layer.size()-1);





    }

    // анализируем проект, собираем кеш
    public ProjectTypesDTO analyzeProject(Directory root, String rootPath){

        System.out.println(root);

        ProjectTypesDTO projectTypesDTO = new ProjectTypesDTO();
        try {

            ArrayList<String> layer = new ArrayList<>();
            layer.add(rootPath);



            collectAndAnalyzeFiles(layer, root, projectTypesDTO);

        }
        catch (Exception e){
            e.printStackTrace();
        }

        return projectTypesDTO;
    }



    // фомрмируем предложку на основе текущего состояния кода, а также контекста
    // сразу фильтруем результат

    public BasicSuggestionContextBasedInfo basicSuggestionContextBasedAnalysis(EditorBasicSuggestionRequest request){
        BasicSuggestionContextBasedInfo info = new BasicSuggestionContextBasedInfo();
        try {
            String completedCode = makeCodeComplete(request);

            CompilationUnit c = parser.parse(completedCode).getResult().orElseThrow(()->new IllegalArgumentException("parsing failed"));
            String packageDeclaration = (c.getPackageDeclaration().orElseThrow(()-> new IllegalArgumentException("no package")))
                    .getNameAsString();
            info.setPackageWay(packageDeclaration);


            // Извлекаем типы внутри рассматриваемого файла. Они доступны везде
            c.getTypes().forEach(t->{
                info.getTypes().add(t.getNameAsString());
            });


            // отыскиваем тип, соответствующий позиции пользователя
            Optional<TypeDeclaration<?>> type = c.getTypes().stream().filter(t->{
                Range r = t.getRange().orElseThrow(()->new IllegalArgumentException("invalid type range"));
                return r.begin.line<= request.getLine()&&r.end.line>= request.getLine();
            }).findFirst();

            // если пользователь совершает действия в диапазоне какого-либо типа
            if (type.isPresent()){
                TypeDeclaration<?> chosenType = type.get();
                // далее - пользователь находится в методе или в теле класса или в конструкторе?
                // если в методе - есть разница между статикой и нестатикой



                List<String> staticMethods = new ArrayList<>();
                List<String> staticFields = new ArrayList<>();
                List<String> nonStaticMethods = new ArrayList<>();
                List<String> nonStaticFields = new ArrayList<>();

                nonStaticMethods.addAll(objectMethods.stream()
                        .filter(el->el.startsWith(request.getText())).toList());

                // собираем поля типа
                chosenType.getFields().forEach(f->{
                    if (f.isStatic()){

                        f.findAll(VariableDeclarator.class).forEach(v->{

                            if (v.getNameAsString().startsWith(request.getText())){
                                staticFields.add(v.getNameAsString());
                            }
                        }
                        );

                    }
                    else {

                        f.findAll(VariableDeclarator.class).forEach(v->{

                                    if (v.getNameAsString().startsWith(request.getText())){
                                        nonStaticFields.add(v.getNameAsString());
                                    }
                        }

                        );

                    }
                });

                Optional<MethodDeclaration> methodDeclaration = chosenType.getMethods().stream().peek(m->{
                    if (m.isStatic() && m.getNameAsString().startsWith(request.getText())){
                        staticMethods.add(m.getNameAsString());
                    }

                    else if (!m.isStatic() && m.getNameAsString().startsWith(request.getText())) {
                        nonStaticMethods.add(m.getNameAsString());
                    }
                }).filter(m->{
                    Range r = m.getRange().orElseThrow(()->new IllegalArgumentException("invalid type range"));
                    return r.begin.line<= request.getLine()&&r.end.line>= request.getLine();
                }).findFirst();

                // пользователь находится вне метода - он может быть в конструкторе или просто в теле...

                if (methodDeclaration.isEmpty()){


                    /*
                    проверка на конструктор

                     */

                    Optional<ConstructorDeclaration> constructorDeclaration = chosenType.getConstructors().stream()
                            .filter(co->{
                                Range r = co.getRange().orElseThrow(()->new IllegalArgumentException("invalid constructor range"));
                                return r.begin.line<= request.getLine()&&r.end.line>= request.getLine();
                            }).findFirst();

                    // вне конструктора
                    if (constructorDeclaration.isEmpty()){
                        List<String> availableKeywords = List.of("abstract", "enum", "record",
                                "transient","int", "interface","private", "public", "protected",
                                "sealed", "short", "static", "synchronized","double", "final", "float",
                                "long","char", "class", "void", "volatile", "boolean", "byte","native","non-sealed"
                        );
                        info.setKeywords(availableKeywords.stream().filter(el->el.startsWith(request.getText())).toList());

                    }

                    // внутри конструктора
                    else {
                        ConstructorDeclaration constructor = constructorDeclaration.get();

                        // добавляем и фильтруем статичные и не статичные методы класса
                        List<String> availableKeywords = List.of("while","enum", "record","this", "try", "throw", "if", "int",
                                "interface", "short", "super", "switch", "synchronized","do", "double", "final", "for", "float",
                                "long", "class", "char","var","boolean", "byte","new"
                                );
                        info.setKeywords(availableKeywords.stream().filter(el->el.startsWith(request.getText())).toList());

                        info.getMethods().addAll(nonStaticMethods);
                        info.getMethods().addAll(staticMethods);
                        info.getFields().addAll(staticFields);
                        info.getFields().addAll(nonStaticFields);


                        // извлекаем параметры
                        constructor.getParameters().forEach(el->{


                            if (el.getNameAsString().startsWith(request.getText())){

                                info.getLocalVariables().add(el.getNameAsString());
                            }
                        });

                        // работаем с телом - помним, что локальные переменные можно вписать только после их объявления
                        BlockStmt body = constructor.getBody();

                        body.findAll(VariableDeclarator.class).forEach(el->{
                            Range r = el.getRange().orElseThrow(()->new IllegalArgumentException("invalid variable range"));
                            if (r.end.line<= request.getLine() && el.getNameAsString().startsWith(request.getText())){
                                info.getLocalVariables().add(el.getNameAsString());
                            }


                        });














                    }



                }
                // пользователь находится внутри метода - два сценария в зависимости от статичности контекста
                else {
                    MethodDeclaration method = methodDeclaration.get();


                    // добавляем и фильтруем статичные и не статичные методы класса
                    List<String> availableKeywords = List.of("while","enum", "record","this", "try", "throw", "if", "int",
                            "interface", "short", "super", "switch", "synchronized","do", "double", "final", "for", "float",
                            "long", "class", "char","var","boolean", "byte","new"
                    );
                    info.setKeywords(availableKeywords.stream().filter(el->el.startsWith(request.getText())).toList());

                    // доступ к статичному контексту типа
                    if (!method.isStatic()){
                        info.getFields().addAll(nonStaticFields);
                        info.getMethods().addAll(nonStaticMethods);
                    }


                    info.getFields().addAll(staticFields);
                    info.getMethods().addAll(staticMethods);

                    // извлекаем параметры
                    method.getParameters().forEach(el->{


                        if (el.getNameAsString().startsWith(request.getText())){

                            info.getLocalVariables().add(el.getNameAsString());
                        }
                    });

                    // работаем с телом - помним, что локальные переменные можно вписать только после их объявления
                    Optional<BlockStmt> body = method.getBody();
                    body.ifPresent(
                            blockStmt -> blockStmt.findAll(VariableDeclarator.class)
                                    .forEach(el ->
                                    {
                                        Range r = el.getRange().orElseThrow(() -> new IllegalArgumentException("invalid variable range"));
                                        if (r.end.line <= request.getLine() && el.getNameAsString().startsWith(request.getText())) {
                                            info.getLocalVariables().add(el.getNameAsString());
                                        }


                    }));




                }




            }

            // если пользователь находится вне типа - ему доступны ключевые слова
            else {
                info.setOutsideOfType(true);
                List<String> availableKeywords = List.of("enum", "record","import", "interface",
                        "public","abstract","sealed","final","class","non-sealed");

                info.setKeywords(availableKeywords.stream().filter(el->el.startsWith(request.getText())).toList());

            }


        }
        catch (Exception e){
            e.printStackTrace();
        }

        return info;
    }





    public EditorBasicSuggestionAnswer basicSuggestion(EditorBasicSuggestionRequest request){
        EditorBasicSuggestionAnswer answer = new EditorBasicSuggestionAnswer();

        try {
            String completedCode = makeCodeComplete(request);

            CompilationUnit c = parser.parse(completedCode).getResult().orElseThrow(()->new IllegalArgumentException("parsing failed"));

            // пример извлечения публичного типа и его методов из кода
            c.getTypes().forEach(el->{


                System.out.println("type "+el.getNameAsString()+" with modifiers "+el.getModifiers());
                el.getMethods().forEach(m->{
                    System.out.println("method "+ m.getName()+" with modifiers "+m.getModifiers());
                });
            });

        }
        catch (Exception e){
            return answer;
        }







        return answer;
    }




    public ImportAnswer importParsing(ImportRequest importRequest){
        //prepareParserConfigForDotSuggestion();

        try {
            HashSet<String> imports = new HashSet<>();
            CompilationUnit c = parser.parse(importRequest.getCode())
                    .getResult().orElseThrow(()->new IllegalArgumentException("parsing failed"));
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

    private String makeCodeComplete(EditorDotSuggestionRequest request){
        return request.getCode().substring(0, request.getPosition())
                +"dummy;"+request.getCode().substring(request.getPosition()+1);
    }

    private String makeCodeComplete(EditorBasicSuggestionRequest request){
        int lineStart = request.getLineStart();
        String code = request.getCode();

        return code.substring(0, lineStart)+"//"+code.substring(lineStart+1);
    }


    public DotSuggestionAnswer dotParsing(DotSuggestionRequest request) {






        DotSuggestionAnswer dotSuggestionAnswer = new DotSuggestionAnswer();



        try {

            String s = makeCodeComplete(request);
            //prepareParserConfigForDotSuggestion();
            // парсим
            CompilationUnit c = parser.parse(s).getResult().orElseThrow(()->new IllegalArgumentException("parsing failed"));
            System.out.println(StaticJavaParser.getParserConfiguration().getSymbolResolver());


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






























}
