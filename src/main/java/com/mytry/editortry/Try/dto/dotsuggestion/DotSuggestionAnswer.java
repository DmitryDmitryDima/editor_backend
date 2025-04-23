package com.mytry.editortry.Try.dto.dotsuggestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;

/*
Данный ответ предназначен для события проставления точки
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DotSuggestionAnswer {

    // методы - в будущем можно разделить на публичные и не очень
    List<String> methods;

    // поля
    List<String> fields;



}
