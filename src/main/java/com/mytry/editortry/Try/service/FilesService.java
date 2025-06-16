package com.mytry.editortry.Try.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FilesService {

    @Value("${files.directory}")
    private String filesDirectory;



    public void loadFile(){

    }


    public void saveFile(){

    }



    public void deleteFile(){

    }

    public void createFile(){

    }



}
