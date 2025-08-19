package com.mytry.editortry.Try.dto.basicsuggestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/*
сущность, относящаяся к кешу.
В зависимости от типа запроса editor service извлекает нужную ему инфу (либо методы, либо название типа)


 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheSuggestionInnerProjectType {

    private String name;

    // по сути копия строки package
    private String packageWay;

    // используются при dot парсинге
    private List<String> methods;

    // используются при dot парсинге
    private List<String> fields;





}
