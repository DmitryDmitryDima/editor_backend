package com.mytry.editortry.Try;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UtilsTest {


    // исследуем новый подход к парсингу -
    // от фронта мы получаем полную строку (а также контекст), после чего исследуем ее парсером

    @Test
    public void testAST(){
        String expression = "hello()";


        Expression parsedEx = StaticJavaParser.parseExpression(expression);

        // сценарий точки после единичной переменной
        if (parsedEx.isNameExpr()){
            System.out.println("dot after variable");
            NameExpr nameExpr = parsedEx.asNameExpr();
            System.out.println(nameExpr.getName().asString());
        }
        // сценарий вызова метода
        else if (parsedEx.isMethodCallExpr()){
            System.out.println("dot after method");
            MethodCallExpr methodCallExpr = parsedEx.asMethodCallExpr();
            System.out.println(methodCallExpr.getName().asString());
        }

    }
}
