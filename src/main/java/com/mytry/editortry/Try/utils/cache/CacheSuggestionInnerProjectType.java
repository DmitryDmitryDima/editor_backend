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
    // нестатические поля
    private List<String> publicMethods = new ArrayList<>();
    private List<String> defaultMethods = new ArrayList<>();
    private List<String> publicFields = new ArrayList<>();
    private List<String> defaultFields = new ArrayList<>();

    // статические поля
    private List<String> publicStaticMethods = new ArrayList<>();
    private List<String> defaultStaticMethods = new ArrayList<>();
    private List<String> publicStaticFields = new ArrayList<>();
    private List<String> defaultStaticFields = new ArrayList<>();

    // вложенные классы - под вопросом
    private List<CacheSuggestionInnerProjectType> innerTypes = new ArrayList<>();


}
