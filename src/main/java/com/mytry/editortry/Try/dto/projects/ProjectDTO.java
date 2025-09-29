package com.mytry.editortry.Try.dto.projects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDTO {

    private Long id;

    private String name;

    private boolean running;




    private DirectoryDTO root;

    private Map<String, FlatTreeMember> flatTree;





    private List<String> projectLogLines = new ArrayList<>();

}
