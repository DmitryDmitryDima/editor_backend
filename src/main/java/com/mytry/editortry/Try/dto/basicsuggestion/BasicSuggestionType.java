package com.mytry.editortry.Try.dto.basicsuggestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
дто для репрезентации типа, внешнего по отношению к анализируемому файлу (из проекта или библиотеки)
В ответе мы также несем строку импорта, реализуя возможность автоимпорта при выборе соответствующего варианта
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasicSuggestionType {

    private String packageWay;
    private String name;
}
