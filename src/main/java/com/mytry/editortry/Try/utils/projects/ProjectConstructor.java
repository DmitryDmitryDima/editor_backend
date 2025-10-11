package com.mytry.editortry.Try.utils.projects;

import com.mytry.editortry.Try.dto.projects.ProjectCreationRequest;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.utils.projects.yaml.DirectoryInstruction;
import com.mytry.editortry.Try.utils.projects.yaml.FileInstruction;
import com.mytry.editortry.Try.utils.projects.yaml.YamlInstruction;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectConstructor {


    @Value("${files.directory}")
    private String disk_location_user_filebase;









    
    
    // вызывается из transactional контекста
    public void buildProject(Project project, ProjectCreationRequest request) throws Exception{

        Directory root = project.getRoot();

        ProjectType type = ProjectType.MAVEN_CLASSIC; // временно задаем этот параметр вручную, в будущем ползователь будет слать инфу




        // загружаем файл инструкцию в зависимости от выбранного типа
        String fileInstruction = switch (type){
            case MAVEN_CLASSIC -> "maven_classic.yaml";
            case GRADLE_CLASSIC -> "gradle_classic.yaml";
        };




        Resource loadedInstructionEntity = new ClassPathResource("/project_build_instructions/"+fileInstruction);

        // читаем инструкцию из внутренних ресурсов проекта
        YamlInstruction yamlInstruction = openYamlInstruction(loadedInstructionEntity.getInputStream());


        // выполняем инструкцию
        runInstruction(yamlInstruction, root);

        // выполняем подготовку для конкретного типа проекта - к примеру в maven форматируется pom.xml
        projectPreparation(type, project, request);

        
        
    }

    // в данном методе. в зависимости от типа, будет реализовываться дополнительная работа над созданной структурой
    private void projectPreparation(ProjectType type, Project project, ProjectCreationRequest request) throws Exception{
        if (type == ProjectType.MAVEN_CLASSIC){
            // в случае с maven необходимо отформатировать pom.xml
            ProjectUtils.setArtifactIdInsidePomXML(project.getRoot().getConstructedPath()+"/"+"pom.xml",
                    project.getName()+"-project");

            // остальные пользовательские параметры
            if (request.getNeedEntryPoint()){
                ProjectUtils.generateEntryPointForMavenProject(project);
            }
        }


    }

    private YamlInstruction openYamlInstruction(InputStream stream){
        Yaml yaml = new Yaml(new Constructor(YamlInstruction.class, new LoaderOptions()));
        return yaml.load(stream);
    }

    /*
    обработка инструкции - внесение шаблона проекта в базу и в файловую систему
    В базу теперь вносятся все сущности, в том числе и невидимые - фильтрация происходит на этапе серверного ответа
     */

    private void runInstruction(@NotNull YamlInstruction instruction, Directory root) throws Exception{



        // parent = null - значит верх иерархии, имеющий прямую зависимость с root. Иерархия строится с директорий
        List<DirectoryInstruction> directoryInstructions = instruction.getDirectories();
        HashMap<Long, Directory> directoriesBase = new HashMap<>();



        directoriesBase.put(null, root);

        Set<Long> higherLayer = new HashSet<>();
        higherLayer.add(null);

        // опускаемся вглубь иерархии. Если инструкция содержит цикл - ловим это счетчиком
        int iteration = 0;
        while (!instruction.getDirectories().isEmpty()){
            // в этой коллекции собираем те директории, которые будут следующим верхним уровнем
            Set<Long> toRemove = new HashSet<>();

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

                    // создаем зависимость в базе
                    ProjectUtils.injectChildToParent(child, parent);

                    // пишем директорию, при этом дополняя path для child
                    ProjectUtils.writeDirectoriesAndCachePath(parent, child);



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
            Directory parent = directoriesBase.get(fileInstruction.getParent());
            if (parent == null) {
                throw new IllegalStateException("instruction contains broken dependency between file and directory");
            }


            // формируем зависимость в бд
            File file = fileInstruction.prepareFile();

            ProjectUtils.injectChildToParent(file, parent);







            // пишем файл, при необходимости выполняя загрузку шаблона
            String fileName = file.getName();

            if (file.getExtension()!=null){
                fileName  =fileName+"."+file.getExtension();
            }

            ProjectUtils.writeFile(Path.of(parent.getConstructedPath(), fileName), fileInstruction.getTemplate());







        }

















    }


    
    
}
