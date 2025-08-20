package com.mytry.editortry.Try.utils.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheSuggestionInnerProjectType {

    private String name;
    // используются для dot парсинга
    private List<String> methods = new ArrayList<>();
    // используются для dot парсинга
    private List<String> fields = new ArrayList<>();
    // вложенные классы - под вопросом
    private List<CacheSuggestionInnerProjectType> innerTypes = new ArrayList<>();


}
