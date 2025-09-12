package com.mytry.editortry.Try.utils.cache;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

// данный класс предназначен для репрезентации типов, извлекаемых из подключенных библиотек
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheSuggestionOuterProjectFile implements Serializable {



    @Serial
    private static  final long serialVersionUID = 1L;

    private String name;

    // генерируется готовый импорт (пакет) - когда пользователь выберет вариант, строка вставится автоматически
    private String packageWay;

    private Set<String> methods = new HashSet<>();
    private Set<String> fields = new HashSet<>();
}
