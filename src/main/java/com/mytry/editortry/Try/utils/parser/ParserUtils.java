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

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserUtils {

    private static final Logger logger = LoggerFactory.getLogger(ParserUtils.class);








    public DotSuggestionAnswer dotParsing(DotSuggestionRequest request) {

        // анализируем строку



        Expression parsedEx = StaticJavaParser.parseExpression(request.getExpression());
        logger.info(parsedEx.toString()+ " parsed ex");

        // сценарий точки после единичной переменной
        if (parsedEx.isNameExpr()){

            try {
                return dotParsingVariable(parsedEx.asNameExpr(), request);
            }
            catch (Exception e){
                e.printStackTrace();
                return new DotSuggestionAnswer();
            }


        }
        // сценарий вызова метода
        else if (parsedEx.isMethodCallExpr()){
            // todo missed logic
            return new DotSuggestionAnswer();
        }

        // тут может быть список дефолтных методов - в случаях выше можно делать merge
        else {
            return new DotSuggestionAnswer();
        }


    }


















    // анализируем ситуацию, когда точка ставится после переменной
    private DotSuggestionAnswer dotParsingVariable(NameExpr parsedExpression, DotSuggestionRequest request) throws Exception{

        // извлекаем имя объекта
        String object = parsedExpression.getName().asString();

        // редактируем код - ставим комментарий (пока что для всей линии)
        String editedContext = makeCodeCompleteDotVariable(object, request);



        prepareParserConfigForDotSuggestion();

        // парсим
        CompilationUnit c = StaticJavaParser.parse(editedContext);


        try {
            FormattedAST formattedAST = new FormattedAST(c);
        }

        catch (Exception e){
            e.printStackTrace();
        }




        //todo тест альтернативного подхода


        // гуляем по ноде, изучаем, что к чему относится
        Node.BreadthFirstIterator iterator = new Node.BreadthFirstIterator(c);
        while (iterator.hasNext()){
            Node node = iterator.next();

            //System.out.println("observing node");

            if (node instanceof Parameter p){
                //System.out.println("parameter->>>>>");
                //System.out.println(p.getType().resolve().describe());

            }

            // тут весь метод - имя, тело, с return'ом. Можно
            if (node instanceof MethodDeclaration md){
                //System.out.println("Method declaration");


            }

            // тут можно узнать, какой это вид выражения
            if (node instanceof Expression exp){
                //System.out.println("expression");
                if (exp instanceof MethodCallExpr methodCallExpr){
                    //System.out.println(methodCallExpr.getName());
                    //System.out.println(methodCallExpr.getParentNode().get());
                }

            }

            // body declaration - это как и метод, так и класс
            // исходя из этого,
            // мы можем отделить диапазоны методов от диапазонов класс и,
            // соответственно, отделить локальные переменные от полей
            if (node instanceof BodyDeclaration<?> bd){
                //System.out.println("body declaration");
                bd.ifClassOrInterfaceDeclaration((cl)->{
                    //System.out.println("class or interface declaration");
                    //System.out.println(cl);

                });

                bd.ifMethodDeclaration(md->{
                    //System.out.println("method declaration");
                    //System.out.println(md.getBody());
                    //System.out.println(md.getName());
                });

                bd.ifConstructorDeclaration(constructorDeclaration -> {
                    //System.out.println("constructor declaration");
                    //System.out.println(constructorDeclaration.getBody());
                });

                bd.ifFieldDeclaration(fieldDeclaration -> {
                    //System.out.println("field declaration");
                    //System.out.println(fieldDeclaration);
                });

            }

            // тип - тут и извлекаются методы и прочее
            if (node instanceof Type type){
                ResolvedType resolved  = type.resolve();
                if (resolved.isReferenceType()){
                    ResolvedReferenceType rt = resolved.asReferenceType();
                    //System.out.println(rt.describe());
                }
            }


            /*
            In int x = 14, y = 3; "int x = 14" and "int y = 3" are VariableDeclarators.
             */
            if (node instanceof VariableDeclarator){
                //System.out.println("variable");
                //System.out.println(node);
            }



        }



















        DotSuggestionAnswer dotSuggestionAnswer = new DotSuggestionAnswer();

        // создаем объект обхода спарсенного кода ля поиска нужного объекта
        DotSuggestionCollector dotSuggestionCollector = new DotSuggestionCollector(object, request);

        dotSuggestionCollector.visit(c, dotSuggestionAnswer);

        return dotSuggestionAnswer;

    }


    /*
    использую заглушку ._ - таким образом я не сломаю контекст.
     */
    private String makeCodeCompleteDotVariable(String object, DotSuggestionRequest request){




        int index = request.getPosition();

        // дальше в зависимости от типа случая, стоит скобка или нет ....

        // Реализация для переменной - курсор стоит после точки, а точка после имени переменной - длина объекта плюс точки
        int objectLength = object.length()+1;


        return request.getCode().substring(0, index)+"dummy;"+request.getCode().substring(index+1);

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
