package com.mytry.editortry.Try.dto.dotsuggestion;


import lombok.*;

@Value
public class DotSuggestionRequest {

    // контекст
    String code;



    // позиция курсора - необходима для контроля контекста
    Integer position;

    //содержимое line без точки - анализируем в парсере
    String expression;

    // номер строки
    int line;


}
