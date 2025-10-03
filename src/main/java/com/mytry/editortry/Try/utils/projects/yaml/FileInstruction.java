package com.mytry.editortry.Try.utils.projects.yaml;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInstruction {
    private String name;
    private boolean immutable;
    private boolean visible;
    private String template;
    private Long parent;

}
