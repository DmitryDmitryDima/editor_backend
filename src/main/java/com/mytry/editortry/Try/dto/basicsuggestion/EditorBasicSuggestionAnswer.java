package com.mytry.editortry.Try.dto.basicsuggestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditorBasicSuggestionAnswer {

    /*
    мгновенная предложка с учетом контекста и текущего состояния файла
     */
    private BasicSuggestionContextBasedInfo contextBasedInfo;


    /*
    предложка из проекта
     */
    private List<BasicSuggestionProjectType> outerTypes;

    /*
    в будущем - предложка из библиотек
     */







}
