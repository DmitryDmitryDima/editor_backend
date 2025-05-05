package com.mytry.editortry.Try.utils.parser;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
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

        // сценарий точки после единичной переменной
        if (parsedEx.isNameExpr()){

            try {
                return dotParsingVariable(parsedEx.asNameExpr(), request);
            }
            catch (Exception e){
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










    // зная код, идущий до точки, а также позицию курсора, мы можем, к примеру, закомментить переменную
    // если точка ставится после метода, мы добавляем ;
    private String makeCodeCompleteDotVariable(String object, DotSuggestionRequest request){




        int index = request.getPosition();

        // дальше в зависимости от типа случая, стоит скобка или нет ....

        // Реализация для переменной - курсор стоит после точки, а точка после имени переменной - длина объекта плюс точки
        int objectLength = object.length()+1;


        return request.getCode().substring(0, index - objectLength) + "//" +
                request.getCode().substring(index - objectLength);
    }




    // конфигурируем парсер (пока не ясно, можно ли сделать эо один раз)
    private void prepareParserConfigForDotSuggestion(){
        // Конфигурация парсера
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

    }

    // анализируем ситуацию, когда точка ставится после переменной
    private DotSuggestionAnswer dotParsingVariable(NameExpr parsedExpression, DotSuggestionRequest request) throws Exception{

        // извлекаем имя объекта
        String object = parsedExpression.getName().asString();

        // редактируем код - ставим комментарий (пока что для всей линии)
        String editedContext = makeCodeCompleteDotVariable(object, request);

        // конфигурируем
        prepareParserConfigForDotSuggestion();

        // парсим
        CompilationUnit c = StaticJavaParser.parse(editedContext);

        DotSuggestionAnswer dotSuggestionAnswer = new DotSuggestionAnswer();

        // создаем объект обхода спарсенного кода ля поиска нужного объекта
        DotSuggestionCollector dotSuggestionCollector = new DotSuggestionCollector(object, request);

        dotSuggestionCollector.visit(c, dotSuggestionAnswer);

        return dotSuggestionAnswer;

    }





}
