package com.mytry.editortry.Try.utils.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


/*
сущность, относящаяся к кешу.
В зависимости от типа запроса editor service извлекает нужную ему инфу (либо методы, либо название типа)


 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheSuggestionInnerProjectFile {


    // по сути копия строки package
    private String packageWay;

    // для удобства отделяем публичный тип от дефолтных (при их наличии)
    private CacheSuggestionInnerProjectType publicType;

    private List<CacheSuggestionInnerProjectType> defaultTypes = new ArrayList<>();

    // обновляем только информацию по типам - прочая информация остается прежней
    public void updateTypeStructureFrom(CacheSuggestionInnerProjectFile cacheSuggestionInnerProjectFile){
        publicType = cacheSuggestionInnerProjectFile.publicType;
        defaultTypes = cacheSuggestionInnerProjectFile.defaultTypes;
    }
}

