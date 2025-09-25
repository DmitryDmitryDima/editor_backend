package com.mytry.editortry.Try.utils.processes;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

// сервис, отвечающий за запись информации от запущенного проекта в лог
@Service
public class ProjectLogger {

    // название лог файла (хранится в каждой из директорий проекта, скрыт от пользователя)
    private final String logFile = "current.log";

    // максимальный размер лога
    private final long maximumLogFileSize = 1024*512; // 512 kb


    // лог очищается при старте программы, а также при достижении максимального размера
    public void clearLog(String path)  {
        try {
            Files.write(Paths.get(path, logFile), new byte[0]);
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear log: " + path, e);
        }

    }

    // добавляет строку к уже существующему содержимому - проверяем, не превышен ли лимит и существует ли файл
    public void addToLog(String message, String destination){



        try {
            checkExistence(destination);
            checkSize(destination);
        }

        catch (Exception e){
            // some actions for system recovery manager
            e.printStackTrace();
        }

        try {
            message = message+ System.lineSeparator();

            Files.write(Paths.get(destination,logFile), message.getBytes(), StandardOpenOption.APPEND);
        }
        catch (Exception e){
            // working with writing error
            e.printStackTrace();
        }





    }


    // загружаем текущее состояние лога
    public void loadLog(String from){

    }

    // проверяем существование файла
    private void checkExistence(String path) throws IOException {
        boolean exists = Files.exists(Paths.get(path, logFile));
        if (!exists){
            Files.createFile(Paths.get(path, logFile));

        }
    }

    // проверяем, не превышен ли лимит
    private void checkSize(String path) throws Exception{
        File file = new File(path+logFile);
        if (file.length()>=maximumLogFileSize){
            clearLog(path);
        }
    }



}
