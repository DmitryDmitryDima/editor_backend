package com.mytry.editortry.Try.utils.projects.yaml;


import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.enums.FileStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInstruction {
    private String name;
    private String extension;
    private boolean immutable;
    private boolean hidden;
    private String template;
    private Long parent;


    public File prepareFile(){
        File file = new File();
        file.setName(name);
        file.setExtension(extension);
        file.setImmutable(immutable);
        file.setCreatedAt(Instant.now());
        file.setStatus(FileStatus.AVAILABLE);
        file.setUpdatedAt(Instant.now());
        file.setHidden(hidden);

        return file;
    }

}
