package com.mytry.editortry.Try.dto.files;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextFileDTO {

    // текстовое содержимое
    private String content;

    // данные для управления сессиями (возможно структура и необходимые данные будут существенно изменены)
    private Long file_id;

    private Long project_id;







}
