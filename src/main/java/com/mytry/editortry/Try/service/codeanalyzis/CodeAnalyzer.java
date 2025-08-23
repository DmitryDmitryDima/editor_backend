package com.mytry.editortry.Try.service.codeanalyzis;



import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import com.github.javaparser.ast.expr.Expression;

import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mytry.editortry.Try.dto.basicsuggestion.BasicSuggestionContextBasedInfo;
import com.mytry.editortry.Try.dto.basicsuggestion.ProjectTypesDTO;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectFile;
import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionAnswer;
import com.mytry.editortry.Try.dto.basicsuggestion.EditorBasicSuggestionRequest;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import com.mytry.editortry.Try.dto.importsuggestion.ImportAnswer;
import com.mytry.editortry.Try.dto.importsuggestion.ImportRequest;
import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;

@Service
public class CodeAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(CodeAnalyzer.class);




    // генерируем публичное api файла (учитываем, что может быть несколько типов) - метод используется как в точечном анализе, так и в глобальном
    public CacheSuggestionInnerProjectFile generateFileCache(String code) throws Exception{
        CacheSuggestionInnerProjectFile file = new CacheSuggestionInnerProjectFile();
        CompilationUnit c = StaticJavaParser.parse(code);

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

    public BasicSuggestionContextBasedInfo basicSuggestionContextBasedAnalysis(EditorBasicSuggestionRequest request){
        BasicSuggestionContextBasedInfo info = new BasicSuggestionContextBasedInfo();
        try {
            String completedCode = makeCodeComplete(request);
            prepareParserConfigForDotSuggestion(); // todo - не ясно, будут ли вообще отличия в конфигурации
            CompilationUnit c = StaticJavaParser.parse(completedCode);

            // пока реализуем просто извлечение типов
            c.getTypes().forEach(el->{
                info.getTypes().add(el.getNameAsString());
            });


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
            prepareParserConfigForDotSuggestion(); // todo - не ясно, будут ли вообще отличия в конфигурации
            CompilationUnit c = StaticJavaParser.parse(completedCode);

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

    private String makeCodeComplete(EditorBasicSuggestionRequest request){
        int lineStart = request.getLineStart();
        String code = request.getCode();

        return code.substring(0, lineStart)+"//"+code.substring(lineStart+1);
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
