package com.mytry.editortry.Try.utils.parser;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserUtils {

    private static final Logger logger = LoggerFactory.getLogger(ParserUtils.class);



    // конфигурируем парсер (пока не ясно, можно ли сделать эо один раз)
    private void prepareParserConfigForDotSuggestion(){
        // Конфигурация парсера
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }


    // зная код, идущий до точки, а также позицию курсора, мы можем, к примеру, закомментить переменную
    // если точка ставится после метода, мы добавляем ;
    private String makeCodeComplete(DotSuggestionRequest request){




        int index = request.getPosition();

        // дальше в зависимости от типа случая, стоит скобка или нет ....

        // Реализация для переменной - курсор стоит после точки, а точка после имени переменной - длина объекта плюс точки
        int objectLength = request.getObject().length()+1;
        String editedCode = request.getCode().substring(0, index - objectLength) + "//" +
                request.getCode().substring(index - objectLength);





        return editedCode;
    }




    public DotSuggestionAnswer dotParsing(DotSuggestionRequest request){
        // сначала нужно попытаться отредактировать код так, чтобы он был корректным для парсера

        String completedCode = makeCodeComplete(request);

        // конфигурируем
        prepareParserConfigForDotSuggestion();

        // парсим
        CompilationUnit c = StaticJavaParser.parse(completedCode);

        DotSuggestionAnswer dotSuggestionAnswer = new DotSuggestionAnswer();

        // создаем объект обхода спарсенного кода ля поиска нужного объекта
        DotSuggestionCollector dotSuggestionCollector = new DotSuggestionCollector(request.getObject());

        dotSuggestionCollector.visit(c, dotSuggestionAnswer);

        return dotSuggestionAnswer;



    }

}
