package com.mytry.editortry.Try.dto.projects;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileCreationRequest {

    private String parentIndex;
    private String suggestion;

}
