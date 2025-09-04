package com.mytry.editortry.Try.dto.files;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditorFileReadAnswer {

    // текстовое содержимое
    private String content;

    // данные для управления сессиями (возможно структура и необходимые данные будут существенно изменены)
    private Long file_id;

    private Long project_id;


    // фиксируем время запроса дял контроля актуальности на стороне фронтенда
    private Instant updatedAt;







}
