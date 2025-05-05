package com.mytry.editortry.Try.utils.parser;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


// Данный класс парсит код. Результат -  map {название переменной:список методов для этой переменной)
// todo если в файле две переменных с одинаковым именем - как найти контекст?
public class DotSuggestionCollector extends VoidVisitorAdapter<DotSuggestionAnswer> {

    private static final Logger logger = LoggerFactory.getLogger(DotSuggestionCollector.class);

    // имя объекта
    private final String objName;


    // строка, где вызван метод
    private final int line;

    public DotSuggestionCollector(String object, DotSuggestionRequest request){
        this.objName = object;
        this.line = request.getLine();
    }

    /* проверяем, совпадает ли контекст для рассматриваемой переменной

    позиция пользователя должна находиться в том же диапазоне, что и позиция рассматриваемого объекта
     */
    private boolean checkContext(VariableDeclarator variableDeclaration){




        /*
        Суть алгоритма - мы рассматриваем объект, добираясь до его первой "диапазонной" ноды.
        Далее смотрим, входит ли позиция курсора юзера (сравниваем линии) в этот диапазон

        Хз, какие могут быть ситуации, поэтому пока поставлю "глубину" прохожения
         */

        if (variableDeclaration.getParentNode().isEmpty()) return false;
        Node parent = variableDeclaration.getParentNode().get();


        int depth= 0;
        while(depth<10){
            int beginLine = parent.getRange().get().begin.line;
            int endLine = parent.getRange().get().end.line;

            if (beginLine!=endLine){
                break;
            }
            else {
                if (parent.getParentNode().isEmpty()) return false;
                else {
                    parent = parent.getParentNode().get();
                    depth++;
                }
            }
        }

        Range range = parent.getRange().get();

        return range.begin.line<=line && range.end.line>=line;


    }

    @Override
    public void visit(VariableDeclarator variableDeclaration, DotSuggestionAnswer answer){


        super.visit(variableDeclaration, answer);




        // при обходе мы работаем только с одной искомой переменной
        // проверяем контекст - во избежание каши из методов при наличии двух переменных с одним именем в разных классах
        if (!variableDeclaration.getName().asString().equals(objName) || !checkContext(variableDeclaration)) return;





        List<String> methods = new ArrayList<>();

        ResolvedType resolvedType = variableDeclaration.getType().resolve();


        // если тип местный, то мы можем вернуть дефолтные методы - смотри параметры фильтра
        String resolvedTypeName = resolvedType.describe();
        logger.info(resolvedTypeName);





        // рассматриваем сценарии с массивом и ссылкой
        try {
            if(resolvedType.isArray()){

                // для массивов можно придумать комбинации со стримами
                methods.addAll(List.of("length", "clone"));
            }

            else {
                // извлекаем методы todo подумай, как можно было бы их отсортировать в порядке ребенок - родитель
                resolvedType.asReferenceType().getAllMethods()
                        .stream().filter(m ->
                                m.accessSpecifier().asString().equals("public")
                                ||
                                (m.accessSpecifier().asString().isEmpty() && !resolvedTypeName.contains(".")))
                        .map(ResolvedDeclaration::getName).distinct()
                        .forEach(methods::add);


            }



        }
        catch (Exception e){
            logger.error(e.getMessage());

        }
        answer.setMethods(methods);

        // todo поля и мб еще что то



    }
}

