package com.mytry.editortry.Try.dto.projects;


import com.mytry.editortry.Try.model.File;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Deque;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSearchInsideProjectResult {

    private Deque<String> path;
    private File file;



}
