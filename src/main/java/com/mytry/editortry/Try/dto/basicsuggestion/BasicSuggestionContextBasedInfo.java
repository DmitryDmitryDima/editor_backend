package com.mytry.editortry.Try.dto.basicsuggestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasicSuggestionContextBasedInfo {

    private List<String> localVariables = new ArrayList<>();
    private List<String> types = new ArrayList<>();
    private List<String> methods = new ArrayList<>();
}
