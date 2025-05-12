package com.mytry.editortry.Try.utils.parser;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormattedAST {

    private final CompilationUnit compilationUnit;// aka root node

    private List<MyClass> classes = new ArrayList<>();

    private Map<String, MyType> foundTypes = new HashMap<>(); //чтобы постоянно не извлекать методы


    public FormattedAST(CompilationUnit compilationUnit){

        this.compilationUnit = compilationUnit;
        constructASTRepresentation();

        foundTypes.forEach((k,v)->
                {
                    System.out.println(k);
                    System.out.println(v.availableMethods);
                }
                );
    }




    public DotSuggestionAnswer explore(DotSuggestionRequest request){

        return null;

    }

    private void constructASTRepresentation(){
        // гуляем по ноде, изучаем, что к чему относится
        Node.BreadthFirstIterator iterator = new Node.BreadthFirstIterator(compilationUnit);

        while (iterator.hasNext()){
            Node node = iterator.next();
            if (node instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration){
                classes.add(new MyClass(classOrInterfaceDeclaration));
            }
        }

    }







    private class MyClass {

        Range classRange;



        List<MyField> fields = new ArrayList<>();
        List<MyMethod> methods = new ArrayList<>();


        MyClass(ClassOrInterfaceDeclaration node) {
            prepareStructure(node);

        }

        private void prepareStructure(ClassOrInterfaceDeclaration node){


            classRange = node.getRange().orElseThrow();

            Node.BreadthFirstIterator iterator = new Node.BreadthFirstIterator(node);

            while (iterator.hasNext()){
                Node child = iterator.next();
                if (child instanceof MethodDeclaration md){
                    methods.add(new MyMethod(md));
                }
                if (child instanceof FieldDeclaration fd){
                    fields.add(new MyField(fd));
                }
            }


        }


    }





    private class MyMethod{

        String methodIdentifier;
        Range methodRange;


        List<MyVariable> locals;

        MyMethod(MethodDeclaration md) {
            prepareStructure(md);

        }


        // разбираем локальные переменные и параметры
        private void prepareStructure(MethodDeclaration md){

        }
    }

    private class MyVariable  {

        MyType type;
        String variableIdentifier;

        MyVariable(Node parent) {
        }
        // тут, по идее, методы
    }

    private class MyType{
        String name;
        List<String> availableMethods = new ArrayList<>();
        List<String> availableFields = new ArrayList<>();
    }



    private class MyConstructor{

        List<MyVariable> params;

        MyConstructor(Node parent) {

        }
    }

    private class MyField {


        String fieldType;
        String fieldIdentifier;

        MyField(FieldDeclaration fieldDeclaration) {
            exploreType(fieldDeclaration);

        }

        // пробуем определить тип и извлечь методы
        private void exploreType(FieldDeclaration fieldDeclaration){
            try {
                ResolvedFieldDeclaration resolvedFieldDeclaration = fieldDeclaration.resolve();

                fieldType = resolvedFieldDeclaration.getType().describe();
                fieldIdentifier = resolvedFieldDeclaration.getName(); // getName - идентификатор

                // если такой тип еще не исследоваля на методы и т д, делаем это
                if (!foundTypes.containsKey(fieldType)){
                    processType(resolvedFieldDeclaration.getType());
                }



            }



            catch (Exception e){
                e.printStackTrace();
            }


        }
    }

    // заносим данные в map
    private void processType(ResolvedType type){

        MyType myType = new MyType();
        myType.name = type.describe();
        if (type.isPrimitive()) return;

        else if (type.isArray()){

            myType.availableMethods.addAll(List.of("length", "clone"));
        }
        else {
            type.asReferenceType().getAllMethods()
                    .stream().filter(m ->
                            m.accessSpecifier().asString().equals("public")
                                    ||
                                    m.accessSpecifier().asString().isEmpty())
                    .map(ResolvedDeclaration::getName).distinct()
                    .forEach(myType.availableMethods::add);
        }

        foundTypes.put(myType.name, myType);




    }


}



