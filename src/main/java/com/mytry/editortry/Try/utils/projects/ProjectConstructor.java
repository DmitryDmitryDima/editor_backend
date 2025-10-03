package com.mytry.editortry.Try.utils.projects;

import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.utils.projects.yaml.DirectoryInstruction;
import com.mytry.editortry.Try.utils.projects.yaml.YamlInstruction;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectConstructor {

    @Value("${files.directory}")
    private String disk_location_user_filebase;

    @Value("${common.directory}")
    private String disk_location_common_system_directory;


    
    
    // вызывается из transactional контекста
    public void buildProject(Directory root, String rootPath, ProjectType type) throws Exception{
        
        // загружаем файл инструкцию в зависимости от выбранного типа
        String fileInstruction = switch (type){
            case MAVEN_CLASSIC -> "maven_classic.yaml";
            case GRADLE_CLASSIC -> "gradle_classic.yaml";
        };

        YamlInstruction yamlInstruction;
        Path path = Path.of(disk_location_common_system_directory+"/instructions/"+fileInstruction);

        // читаем инструкцию
        try (InputStream inputStream = Files.newInputStream(path)) {

            yamlInstruction = openYamlInstruction(inputStream);

        }
        // выполняем инструкцию
        runInstruction(yamlInstruction, root, rootPath);




        // выполняем форматирование для конкретного типа проекта - к примеру в maven форматируется pom.xml

        
        
    }

    private YamlInstruction openYamlInstruction(InputStream stream){
        Yaml yaml = new Yaml(new Constructor(YamlInstruction.class, new LoaderOptions()));
        return yaml.load(stream);
    }

    private void runInstruction(@NotNull YamlInstruction instruction, Directory root, String rootPath){
        // parent = null - значит верх иерархии, имеющий прямую зависимость с root. Иерархия строится с директорий
        List<DirectoryInstruction> directoryInstructions = instruction.getDirectories();

        // извлекаем самую верхную иерархию в виде айди
        List<Long> higherLayer = directoryInstructions
                .stream()
                .filter(directoryInstruction -> directoryInstruction.getParent() == null)
                .map(DirectoryInstruction::getId).toList();
        System.out.println("initial higher level "+higherLayer);
        /*
        List<DirectoryInstruction> initialRemove = new ArrayList<>();
        for (DirectoryInstruction di:directoryInstructions){
            if (higherLayer.contains(di.getId())){
                initialRemove.add(di);
            }
        }

        directoryInstructions.removeAll(initialRemove);

         */

        int iteration = 0;
        while (!instruction.getDirectories().isEmpty()){
            List<Long> toRemove = new ArrayList<>();
            for (DirectoryInstruction directoryInstruction:directoryInstructions){
                if (higherLayer.contains(directoryInstruction.getParent())){
                    toRemove.add(directoryInstruction.getId());
                }
            }
            System.out.println("to remove ");


            instruction.getDirectories().removeIf(directoryInstruction -> toRemove
                    .contains(directoryInstruction.getId()));
            higherLayer = toRemove;
            System.out.println(directoryInstructions);
            System.out.println(higherLayer);

            iteration++;
            if (iteration>=20){
                throw new IllegalStateException("everlasting loop");
            }
        }





    }
    
    
}
