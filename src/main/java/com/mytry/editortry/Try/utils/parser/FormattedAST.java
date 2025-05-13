package com.mytry.editortry.Try.utils.parser;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.Type;
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

        /*
        System.out.println("found info");

        classes.forEach(cl->{
            System.out.println("class with name "+cl.className);
            cl.methods.forEach(m->{
                System.out.println("method with name "+m.methodIdentifier+" has locals:");
                m.locals.forEach(l->{
                    System.out.println(l.variableIdentifier+" with type "+l.type);
                    System.out.println("this type has this methods");
                    foundTypes.get(l.type).availableMethods.forEach(System.out::println);
                });

            });

            cl.fields.forEach(myField -> {
                System.out.println("Field "+myField.fieldIdentifier+" with type "+myField.fieldType);
                System.out.println("this type has this methods");
                foundTypes.get(myField.fieldType).availableMethods.forEach(System.out::println);

            });
        });

         */
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
        String className;



        List<MyField> fields = new ArrayList<>();
        List<MyMethod> methods = new ArrayList<>();


        MyClass(ClassOrInterfaceDeclaration node) {
            prepareStructure(node);

        }

        private void prepareStructure(ClassOrInterfaceDeclaration node){


            classRange = node.getRange().orElseThrow();
            className = node.getNameAsString();


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
        String returnType; // пригодится для подсказок
        String modifier;
        String accessModifier;



        List<MyVariable> locals = new ArrayList<>();


        MyMethod(MethodDeclaration md) {
            prepareStructure(md);

        }


        // разбираем локальные переменные и параметры
        private void prepareStructure(MethodDeclaration md){
            // извлекаем модификатор
            methodIdentifier = md.getName().getIdentifier();

            // извлекаем диапазон
            methodRange = md.getRange().orElseThrow();

            // todo сортировка модификаторов

            // извлекаем свойства метода
            ResolvedType type = md.getType().resolve();
            returnType = type.describe();
            processType(type);
            accessModifier = md.getAccessSpecifier().asString();
            // работаем с параметрами
            md.getParameters().forEach(el->{
                String name = el.getName().asString();
                ResolvedType rt = el.getType().resolve();

                MyVariable myVariable = new MyVariable(rt.describe(), name);
                processType(rt);
                locals.add(myVariable);
            });

            // работаем с телом
            if (md.getBody().isEmpty()) return;


            Node.BreadthFirstIterator iterator = new Node.BreadthFirstIterator(md.getBody().get());
            while (iterator.hasNext()){
                Node node = iterator.next();

                if (node instanceof VariableDeclarator vd){
                    ResolvedType resolvedType = vd.getType().resolve();
                    String name = vd.getNameAsString();

                    locals.add(new MyVariable(resolvedType.describe(), name));
                    processType(type);
                }

                // todo разбираем все выражения - в идеале каждый элемент должен быть обладателем MyVariable или отдельного типа
                // таким образом, при вызове метода мы сможем буквально его найти в контексте

                if (node instanceof Expression expression){

                    // method chaining
                    if (expression instanceof MethodCallExpr methodCall){
                        System.out.println("method call");
                        System.out.println(methodCall);

                        // todo System.out.println() -> java.oi.PrintStream
                        System.out.println(methodCall.getScope().get().calculateResolvedType().describe());

                        System.out.println(methodCall.resolve());

                    }

                    if (expression instanceof NameExpr nameExpr){
                        System.out.println("name expression");
                        System.out.println(nameExpr);

                        System.out.println(nameExpr.resolve().getType().describe()); // тут ошибка
                    }

                    if (expression instanceof FieldAccessExpr fieldAccessExpr){
                        System.out.println("field access expression");
                        System.out.println(fieldAccessExpr.getName().asString());
                        System.out.println(fieldAccessExpr.getScope().calculateResolvedType().describe());

                    }
                }










            }

            //System.out.println(modifier);




        }
    }

    private class MyVariable  {

        String type;
        String variableIdentifier;

        MyVariable(String type, String variableIdentifier) {
            this.variableIdentifier = variableIdentifier;
            this.type = type;
        }

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
                processType(resolvedFieldDeclaration.getType());




            }



            catch (Exception e){
                e.printStackTrace();
            }


        }
    }

    // заносим данные в map
    private void processType(ResolvedType type){

        // если такой тип еще не исследоваля на методы и т д, делаем это
        if (foundTypes.containsKey(type.describe())){
            return;
        }

        MyType myType = new MyType();
        myType.name = type.describe();
        if (type.isPrimitive() || type.isVoid()) return;

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



