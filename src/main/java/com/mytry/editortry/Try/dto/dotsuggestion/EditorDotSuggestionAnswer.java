package com.mytry.editortry.Try.dto.dotsuggestion;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditorDotSuggestionAnswer {

    private List<String> methods;

    // пока что строки, мб понадобится более сложная структура
    private List<String> fields;
}
