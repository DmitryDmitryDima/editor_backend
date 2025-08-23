package com.mytry.editortry.Try.dto.basicsuggestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditorBasicSuggestionRequest {

    private String text;
    private String fullPath;
    private String code;
    private Integer line;
    private Long project_id;
    private Long file_id;
    private Integer lineStart;



}
