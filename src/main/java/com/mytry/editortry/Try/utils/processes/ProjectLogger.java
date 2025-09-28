package com.mytry.editortry.Try.utils.processes;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// сервис, отвечающий за запись информации от запущенного проекта в лог
@Service
public class ProjectLogger {

    // название лог файла (хранится в каждой из директорий проекта, скрыт от пользователя)
    private final String logFileName = "current.log";

    // максимальный размер лога
    private final long maximumLogFileSize = 1024*512; // 512 kb

    // механизм блокировок, базирующийся на айди процесса. Для каждого процесса - свой блокировщик на чтение и запись
    private ConcurrentHashMap<Long, ReentrantLock> filesFineGrainedLockBase = new ConcurrentHashMap<>();


    // лог очищается при старте программы, а также при достижении максимального размера
    public void clearLog(Long projectId,String folderPath)  {
        lock(projectId);
        try {
            clearLog(folderPath);
        }
        finally {
            unlock(projectId);
        }
    }

    private void clearLog(String folderPath){
        try {
            Files.write(Paths.get(folderPath, logFileName), new byte[0]);
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear log: " + folderPath, e);
        }
    }

    // добавляет строку к уже существующему содержимому - проверяем, не превышен ли лимит и существует ли файл
    public void addToLog(String message, Long projectId, String folderPath){


        lock(projectId);

        try {
            checkExistence(folderPath);
            checkSize(folderPath);

            message = message+ System.lineSeparator();
            Files.write(Paths.get(folderPath, logFileName), message.getBytes(), StandardOpenOption.APPEND);
        }

        catch (Exception e){
            // some actions for system recovery manager
            e.printStackTrace();
        }

        finally {
            unlock(projectId);
        }









    }


    // загружаем текущее состояние лога
    public String loadLog(Long projectId, String folderPath){
        lock(projectId);
        try {
            return Files.readString(Path.of(folderPath, logFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            unlock(projectId);
            // clearLockObject(projectId); // маленькая вероятность утечки
        }

    }

    // проверяем существование файла
    private void checkExistence(String folderPath) throws IOException {
        boolean exists = Files.exists(Paths.get(folderPath, logFileName));
        if (!exists){
            Files.createFile(Paths.get(folderPath, logFileName));

        }
    }

    // проверяем, не превышен ли лимит
    private void checkSize(String folderPath) throws Exception{
        long size = Files.size(Paths.get(folderPath, logFileName));

        if (size>=maximumLogFileSize){
            clearLog(folderPath);
        }
    }

    private void lock(Long projectId){
        ReentrantLock lock = filesFineGrainedLockBase.computeIfAbsent(projectId, (k)->new ReentrantLock());
        lock.lock();
    }

    // мы не можем заанлочить процесс, у которого нет лока, поэтому делаем без проверки на отсутствие
    private void unlock(Long projectId){
        ReentrantLock lock = filesFineGrainedLockBase.get(projectId);
        lock.unlock();
    }

    // зачищаем объект блокировки, чтобы не было захламления памяти
    public void clearLockObject(Long projectId){
        filesFineGrainedLockBase.remove(projectId);
    }





}
