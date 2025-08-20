package com.mytry.editortry.Try.utils.cache;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// данный класс предназначен для репрезентации типов, извлекаемых из подключенных библиотек
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheSuggestionOuterProjectFile {

    private String name;

    // генерируется готовый импорт - когда пользователь выберет вариант, строка вставится автоматически
    private String importWay;
}
