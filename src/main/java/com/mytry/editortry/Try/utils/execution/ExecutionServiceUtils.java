package com.mytry.editortry.Try.utils.execution;

import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;

import java.util.*;

public class ExecutionServiceUtils {


    // обход дисковой структуры с помощью deep first seacrh
    public static Optional<File> findFileInsideProject(Directory root, Long fileId){

        Deque<Directory> stack = new ArrayDeque<>();
        // используем id для того, чтобы не зависеть от имплементации hashcode
        Set<Long> visited = new HashSet<>();


        stack.add(root);
        while (!stack.isEmpty()){
            Directory directory = stack.pop();
            if (!visited.contains(directory.getId())){
                // проверяем файлы
                Optional<File> file = directory.getFiles()
                        .stream()
                        .filter(fileEntity->fileEntity.getExtension().equals("java")
                                &&
                                fileEntity.getId().equals(fileId)).findFirst();

                if (file.isPresent()) return file;

                visited.add(directory.getId());
                directory.getChildren().forEach(stack::push);
            }

        }
        return Optional.empty();
    }
}
