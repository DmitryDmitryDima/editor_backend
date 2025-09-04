package com.mytry.editortry.Try.dto.files;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditorFileReadRequest {


    @NotBlank(message = "username is blank")
    private String username;

    @NotBlank(message = "projectname is blank")
    private String projectname;

    @NotBlank(message = "file path is blank")
    private String fullPath;

}
