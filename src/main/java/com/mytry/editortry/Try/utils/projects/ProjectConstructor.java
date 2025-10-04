package com.mytry.editortry.Try.utils.projects;

import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.utils.projects.yaml.DirectoryInstruction;
import com.mytry.editortry.Try.utils.projects.yaml.FileInstruction;
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

    /*
    обработка инструкции - внесение шаблона проекта в базу и в файловую систему
    В базу теперь вносятся все сущности, в том числе и невидимые - фильтрация происходит на этапе серверного ответа
     */

    private void runInstruction(@NotNull YamlInstruction instruction, Directory root, String rootPath) throws Exception{

        System.out.println(instruction);

        // parent = null - значит верх иерархии, имеющий прямую зависимость с root. Иерархия строится с директорий
        List<DirectoryInstruction> directoryInstructions = instruction.getDirectories();
        HashMap<Long, Directory> directoriesBase = new HashMap<>();
        List<Directory> rootLayer = new ArrayList<>(); // сохраняем root сущности для дальнейшего формирования файловых путей
        HashMap<File, String> templateBase = new HashMap<>();

        // извлекаем самый верхний уровень иерархии, связанный напрямую с root directory проекта
        // помимо этого создаем зависимость от айди для сущностей базы данных и инструкции
        List<Long> higherLayer = directoryInstructions
                .stream()
                .filter(directoryInstruction -> directoryInstruction.getParent() == null)
                .peek(directoryInstruction -> {
                    Directory directory = directoryInstruction.prepareDirectoryEntity();
                    rootLayer.add(directory); // сохраняем самый верхний уровень отдельно
                    ProjectUtils.injectChildToParent(directory, root);
                    directoriesBase.put(directoryInstruction.getId(), directory);
                })
                .map(DirectoryInstruction::getId).toList();




        // очищаем базу директорий от верхнего уровня
        List<DirectoryInstruction> initialRemove = new ArrayList<>();
        for (DirectoryInstruction di:directoryInstructions){
            if (higherLayer.contains(di.getId())){
                initialRemove.add(di);
            }
        }

        directoryInstructions.removeAll(initialRemove);


        // опускаемся вглубь иерархии. Если инструкция содержит цикл - ловим это счетчиком
        int iteration = 0;
        while (!instruction.getDirectories().isEmpty()){
            // в этой коллекции собираем те директории, которые будут следующим верхним уровнем
            List<Long> toRemove = new ArrayList<>();

            // ищем детей текущего верхнего уровня, вставляем зависимости
            for (DirectoryInstruction directoryInstruction:directoryInstructions){

                if (higherLayer.contains(directoryInstruction.getParent())){

                    // найденный элемент уходит в будущий верхний уровень иерархии
                    toRemove.add(directoryInstruction.getId());

                    Directory parent = directoriesBase.get(directoryInstruction.getParent());
                    if (parent == null){
                        throw new IllegalStateException("instruction contains broken dependency between" +
                                " directory and directory. Check your id's");
                    }

                    Directory child = directoryInstruction.prepareDirectoryEntity();

                    ProjectUtils.injectChildToParent(child, parent);


                    directoriesBase.put(directoryInstruction.getId(), child);

                }
            }


            // очищаем инструкции от элементов нового верхнего уровня
            instruction.getDirectories().removeIf(directoryInstruction -> toRemove
                    .contains(directoryInstruction.getId()));
            // обновляем верхний уровень
            higherLayer = toRemove;


            iteration++;
            if (iteration>=20){
                throw new IllegalStateException("instruction contains cycle or exceed allowed limit of file structure depth");
            }


        }


        // работаем с файлами
        List<FileInstruction> fileInstructions = instruction.getFiles();
        for (FileInstruction fileInstruction:fileInstructions){
            Directory parent;
            if (fileInstruction.getParent()==null){
                parent = root;
            }
            else {
                parent = directoriesBase.get(fileInstruction.getParent());
                if (parent == null) {
                    throw new IllegalStateException("instruction contains broken dependency between file and directory");
                }
            }

            File file = fileInstruction.prepareFile();

            ProjectUtils.injectChildToParent(file, parent);

            // сохраняем адрес шаблона для следующего шага
            if (fileInstruction.getTemplate()!=null){
                templateBase.put(file, fileInstruction.getTemplate());
            }



        }





    }
    
    
}
