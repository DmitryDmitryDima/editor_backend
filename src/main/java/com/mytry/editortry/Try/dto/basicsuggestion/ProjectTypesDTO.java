package com.mytry.editortry.Try.dto.basicsuggestion;

import com.mytry.editortry.Try.utils.cache.CacheSuggestionInnerProjectFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// формируем многоступенчатую структуру типов в проекте, с возможностью доступа к файул по id
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectTypesDTO {

    // ассоциация - пакет = файлы
    private Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation = new HashMap<>();

    // ассоциация - айди файла = файл
    private Map<Long, CacheSuggestionInnerProjectFile> idToFileAssociation = new HashMap<>();

}
