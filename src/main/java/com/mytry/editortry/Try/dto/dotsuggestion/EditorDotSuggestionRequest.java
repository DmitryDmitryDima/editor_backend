package com.mytry.editortry.Try.dto.dotsuggestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditorDotSuggestionRequest {


    private String code;

    // позиция курсора - необходима для контроля контекста
    private Integer position;

    //содержимое line без точки - анализируем в парсере
    private String expression;

    // номер строки
    private Integer line;

    private Long project_id;
    private Long file_id;

    private Integer column;
}
