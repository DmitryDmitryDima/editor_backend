package com.mytry.editortry.Try.service;

import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;



/*
в будущем - запуск компилятора (и базы данных) через докер

реализация websocket для связи с компилятором - таким образом можно будет запускать программы с бесконечным циклом

 */
@Service
public class CompilerService {


    private final JavaCompiler compiler;
    public static final int TIMEOUT = 5; // SECONDS

    public CompilerService(){
        compiler = ToolProvider.getSystemJavaCompiler();
    }

    /*

    в начале будет создаваться временная директория, однако после за каждым юзером будет закреплена файловая система

     */
    public String makeCompilation(String code) throws Exception{

        Path tempDir = null;

        try {

            // пишем код во временный java file
            tempDir = Files.createTempDirectory("java-code-execution");
            File sourceFile =new File(tempDir.toFile(), "Main.java");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile))) {
                writer.write(code);
            }

            // компилируем код с помощью compiler и в случае ошибки возвращаем ошибку
            String absPath = sourceFile.getAbsolutePath();


            // тут мы можем узнать конкретную причину, почему компиляция провалилась
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            int result = compiler.run(null, outputStream, errorStream, absPath);



            if (result!=0) return errorStream.toString();

            // в случае успешной компиляции запускаем байт код и ловим потенциальный runtime error
            // Ставим лимит на выполнение операции
            // Внимание на имя класса - оно должно совпадать!!!



            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java", "-cp", tempDir.toString(), "Main"
            );


            //

            processBuilder.redirectErrorStream(false); // Keep stderr separate from stdout

            Process process = processBuilder.start();

            // Capture stdout and stderr in separate threads
            ByteArrayOutputStream runtimeOutput = new ByteArrayOutputStream();
            ByteArrayOutputStream runtimeError = new ByteArrayOutputStream();

            Thread outputThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                        runtimeOutput.write(buffer, 0, bytesRead);
                        // System.out.println(runtimeOutput); // вот тут можно реализовать websocket связь
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Thread errorThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = process.getErrorStream().read(buffer)) != -1) {
                        runtimeError.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            errorThread.start();

            // Wait for the process to complete with a timeout
            boolean finished = process.waitFor(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS); // 5-second timeout
            if (!finished) {
                process.destroy(); // Terminate the process

                // тут я присоединяю все то, что попало в консоль до ошибки
                return runtimeOutput+  "\n Execution timed out after 5 seconds.";
            }

            // Check the exit code
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // Join threads to ensure all output is captured
                outputThread.join();
                errorThread.join();

                // Return the error message
                String errorMessage = runtimeError.toString();
                if (errorMessage.isEmpty()) {
                    errorMessage = "Execution failed with exit code: " + exitCode;
                }
                return "Execution failed:\n" + errorMessage;
            }

            // Join threads to ensure all output is captured
            outputThread.join();
            errorThread.join();


            return runtimeOutput.toString();




        }

        catch (Exception e) {
            return "Error "+e.getMessage();
        }

        finally {
            // чистим временное хранилище

            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted((p1, p2) -> -p1.compareTo(p2))
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }



    }
}
