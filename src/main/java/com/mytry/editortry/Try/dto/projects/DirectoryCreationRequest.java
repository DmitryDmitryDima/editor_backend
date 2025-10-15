package com.mytry.editortry.Try.dto.projects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirectoryCreationRequest {

    private String parentIndex;
    private String suggestion;
}
