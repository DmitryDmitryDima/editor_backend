package com.mytry.editortry.Try.utils.projects.yaml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// класс для репрезентации инструкции yaml

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YamlInstruction {
    private List<DirectoryInstruction> directories;
    private List<FileInstruction> files;
}
