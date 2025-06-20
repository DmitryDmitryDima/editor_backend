package com.mytry.editortry.Try.dto.projects;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirectoryDTO {

    private Long id;

    private String name;

    private List<DirectoryDTO> children;

    /*
    private List<DirectoryDTO> sub;

    private List<FileDTO> files;
     */
}
