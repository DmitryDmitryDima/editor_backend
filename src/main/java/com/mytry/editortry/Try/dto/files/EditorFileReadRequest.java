package com.mytry.editortry.Try.dto.files;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditorFileReadRequest {

    private String username;
    private String projectname;
    private String fullPath;

}
