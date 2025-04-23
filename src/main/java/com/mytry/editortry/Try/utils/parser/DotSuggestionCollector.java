package com.mytry.editortry.Try.utils.parser;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


// Данный класс парсит код. Результат -  map {название переменной:список методов для этой переменной)
// todo если в файле две переменных с одинаковым именем - как найти контекст?
public class DotSuggestionCollector extends VoidVisitorAdapter<DotSuggestionAnswer> {

    // имя объекта
    private final String objName;

    public DotSuggestionCollector(String name){
        this.objName = name;
    }

    @Override
    public void visit(VariableDeclarator variableDeclaration, DotSuggestionAnswer answer){


        super.visit(variableDeclaration, answer);


        // при обходе мы работаем только с одной искомой переменной
        if (!variableDeclaration.getName().asString().equals(objName)) return;




        List<String> methods = new ArrayList<>();

        ResolvedType resolvedType = variableDeclaration.getType().resolve();



        // рассматриваем сценарии с массивом и ссылкой
        try {
            if(resolvedType.isArray()){

                // для массивов можно придумать комбинации со стримами
                methods.addAll(List.of("length", "clone"));
            }

            else {
                // извлекаем методы
                resolvedType.asReferenceType().getAllMethods()
                        .stream().filter(m -> m.accessSpecifier().asString().equals("public") ||
                                m.accessSpecifier().asString().isEmpty())
                        .map(ResolvedDeclaration::getName).distinct()
                        .forEach(methods::add);


            }



        }
        catch (Exception e){
            e.printStackTrace();

        }
        answer.setMethods(methods);

        // todo поля и мб еще что то



    }
}

