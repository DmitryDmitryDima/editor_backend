package com.mytry.editortry.Try.dto.projects;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCreationRequest {

    private String projectName;

    // галочка "сгенерировать главный класс"
    private Boolean needEntryPoint;

    private String projectType; // в будущем можно будет выбрать шаблон проекта
}
