package com.mytry.editortry.Try.utils.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class DedicatedJavaParser {

    // todo для создания отдельного instance для каждой из операций
    public static JavaParser getInstance(){
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());


        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);


        ParserConfiguration config = new ParserConfiguration();

        config.setSymbolResolver(symbolSolver);
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

        return new JavaParser(config);
    }
}
