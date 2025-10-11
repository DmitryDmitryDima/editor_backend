package com.mytry.editortry.Try.utils.projects;

import com.mytry.editortry.Try.dto.projects.FileSearchInsideProjectResult;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.model.enums.FileStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileSystemUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ProjectUtils {

    public final static List<String> mavenFolderStructure = List.of("src", "main","java","com");







    private ProjectUtils(){}




    public static Directory getMavenClassicalStructureRoot(Directory projectRoot){
        Directory current = projectRoot;
        for (String sysDir:mavenFolderStructure){

            Optional<Directory> found = current.getChildren().stream().filter(dir->dir.getName().equals(sysDir)).findAny();
            if (found.isEmpty()) throw new IllegalStateException("this is not traditional maven structure");
            current = found.get();
        }
        return current;
    }

    public static String createPathFromAccumulatedCollection(Collection<String> collectionWithPath){
        StringBuilder sb = new StringBuilder();
        for (String part:collectionWithPath){
            sb.append(part).append("/");
        }
        return sb.toString();
    }

    public static void injectChildToParent(Directory child, Directory parent){
        parent.getChildren().add(child);
        child.setParent(parent);
    }
    public static void injectChildToParent(File file, Directory parent){
        parent.getFiles().add(file);
        file.setParent(parent);
    }

    // пишем директории на диск и кешируем путь для child
    public static void writeDirectoriesAndCachePath(Directory parent, Directory child) throws Exception{

        Path childPath = Path.of(parent.getConstructedPath(), child.getName());
        child.setConstructedPath(childPath.toString());

        Files.createDirectories(childPath);
    }

    // генерируем главный файл для мавен проекта
    public static void generateEntryPointForMavenProject(Project project) throws Exception {
        Directory com = getMavenClassicalStructureRoot(project.getRoot());

        File main = new File();
        main.setCreatedAt(Instant.now());
        main.setUpdatedAt(Instant.now());
        main.setName("Main");
        main.setExtension("java");
        main.setStatus(FileStatus.AVAILABLE);

        main.setParent(com);
        com.getFiles().add(main);
        project.setEntryPoint(main);








        String formattedTemplate = """
                        %s
                       \s
                        public class %s{
                          public static void main(String...args){
                           \s
                          }
                       \s
                        }
                       \s"""
                .formatted("package com;","Main");

        Files.writeString(Path.of(com.getConstructedPath(), "Main.java"), formattedTemplate);

        setMainClassInsidePomXML(project.getRoot().getConstructedPath()+"/pom.xml", "com.Main");




    }



    // пишем файл на диск, при необходимости вставляем шаблон
    public static void writeFile(Path filePath, String templateName) throws Exception {
        Files.createFile(filePath);

        if (templateName!=null){
            Resource loadedTemplateEntity = new ClassPathResource("/project_files_templates/"+templateName);
            String templateContent =new String(loadedTemplateEntity.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(filePath, templateContent);
        }
    }



    // обход дисковой структуры с помощью deep first seacrh с формированием пути к файлу от root
    public static Optional<FileSearchInsideProjectResult> findFileInsideProjectWithTrace(Directory root, Long fileId){

        Deque<Directory> stack = new ArrayDeque<>();
        // используем id для того, чтобы не зависеть от имплементации hashcode
        Set<Long> visited = new HashSet<>();

        // с помощью словаря формируем зависимости между директориями

        HashMap<Directory, Directory> directoryTracing = new HashMap<>();


        stack.add(root);
        while (!stack.isEmpty()){
            Directory directory = stack.pop();
            System.out.println(directory.getName());
            if (!visited.contains(directory.getId())){
                // проверяем файлы
                Optional<File> file = directory.getFiles()
                        .stream()
                        .filter(fileEntity->
                                fileEntity.getId().equals(fileId)).findFirst();

                if (file.isPresent()) {

                    Deque<String> path = new ArrayDeque<>();
                    Directory current = directory;
                    while (current!=null){
                        path.offerFirst(current.getName());
                        current = directoryTracing.get(current);
                    }







                    return Optional.of(new FileSearchInsideProjectResult(path, file.get()));
                }

                visited.add(directory.getId());

                for (Directory dir:directory.getChildren()){
                    stack.push(dir);

                    directoryTracing.put(dir, directory);
                }


            }

        }


        return Optional.empty();
    }

    // вставляем artifact id в pom.xml
    public static void setArtifactIdInsidePomXML(String pomPath, String artifactId) throws Exception{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = factory
                .newDocumentBuilder()
                .parse(pomPath);

        document.getDocumentElement().normalize();

        NodeList nodes = document.getElementsByTagName("artifactId");



        Node artifactidNode = nodes.item(0).getFirstChild();
        artifactidNode.setNodeValue(artifactId);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        Result output = new StreamResult(new java.io.File(pomPath));
        Source input = new DOMSource(document);
        transformer.transform(input, output);

    }

    // редактируем pom.xml, вставляя имя главного класса
    public static void setMainClassInsidePomXML(String pomPath, String mainClassName) throws Exception {
        System.out.println(pomPath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = factory
                .newDocumentBuilder()
                .parse(pomPath);

        document.getDocumentElement().normalize();

        NodeList nodes = document.getElementsByTagName("mainClass");

        if (nodes.getLength()!=1){
            throw new IllegalStateException("invalid xml structure");
        }
        Node mainClassNode = nodes.item(0).getFirstChild();
        mainClassNode.setNodeValue(mainClassName);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        Result output = new StreamResult(new java.io.File(pomPath));
        Source input = new DOMSource(document);
        transformer.transform(input, output);


    }

    public static void removeDirectoryFromDisk(Path path) throws Exception{
        FileSystemUtils.deleteRecursively(path);
    }
}
