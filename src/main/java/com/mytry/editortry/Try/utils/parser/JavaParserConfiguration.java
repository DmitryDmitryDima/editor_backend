package com.mytry.editortry.Try.utils.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JavaParserConfiguration {




    @Bean
    public ParserConfiguration prepareParserConfig(){
        // Конфигурация парсера

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());


        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);


        ParserConfiguration config = new ParserConfiguration();

        config.setSymbolResolver(symbolSolver);
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        return config;




    }

    @Bean
    public JavaParser parser(ParserConfiguration configuration){
        return new JavaParser(configuration);
    }

}
