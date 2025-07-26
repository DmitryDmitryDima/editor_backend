package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.files.TextFileDTO;
import com.mytry.editortry.Try.model.Directory;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.Project;
import com.mytry.editortry.Try.repository.ProjectRepository;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

@Service
public class FilesService {

    @Value("${files.directory}")
    private String disk_directory;

    @Autowired
    private ProjectRepository projectRepository;



    public TextFileDTO loadFile(String username,
                                String projectname,
                                String fullPath){


        Project project = projectRepository.findByOwnerUsernameAndName(username, projectname)
                .orElseThrow(()-> new IllegalArgumentException("no project found")
        );

        TextFileDTO textFileDTO = new TextFileDTO();
        textFileDTO.setProject_id(project.getId());

        /*
        извлекаем file_id, заодно проверяя целостность файловой системы

         */
        //System.out.println(fullPath);
        String[] path = fullPath.split("/");
        System.out.println(Arrays.toString(path));
        //System.out.println(path.length);

        Directory directory = project.getRoot();

        File file = null;

        for (int x = 0; x<path.length; x++){
            String step = path[x];
            // сравниваем файлы
            if (x== path.length-1){
                List<File> files = directory.getFiles();
                file = files.stream().filter(el->(el.getName()+"."+el.getExtension())
                        .equals(step))
                        .findAny()
                        .orElseThrow(()->
                        new IllegalArgumentException("no file found")
                );



            }

            else {
                List<Directory> children = directory.getChildren();
                String parent = directory.getName();
                //System.out.println("step "+step);
                //System.out.println(children);
                directory = children.stream().filter(el->el.getName().equals(step)).findAny().orElseThrow(()->
                        new IllegalArgumentException("on parent "+parent +" no directory with name "+step+ " inside "+children)
                );
            }
        }

        textFileDTO.setFile_id(file.getId());

        // загружаем файл из диска
        java.io.File disk_file = new java.io.File(disk_directory+"/"+username+"/projects/"+projectname+"/"+fullPath);
        StringBuilder sb = new StringBuilder();

        try (FileReader r = new FileReader(disk_file);
             BufferedReader bufferedReader = new BufferedReader(r);
        ){

            String s;
            while ((s = bufferedReader.readLine())!=null){
                sb.append(s);
            }
        }
        catch (Exception e){
            throw new IllegalArgumentException("error while reading disk");
        }



        textFileDTO.setContent(sb.toString());








        return textFileDTO;
    }







    public void saveFile(){

    }







}
