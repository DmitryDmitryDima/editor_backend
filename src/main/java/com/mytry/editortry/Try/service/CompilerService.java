package com.mytry.editortry.Try.service;

import com.mytry.editortry.Try.dto.run.RunRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;



/*

Сервис, отвечающий за запуск кода, с получением результата для дальнейшего вывода в консоль

!!! На данный момент работает, создавая временный файл - в дальнейшем будет читать пользовательский файл
Также отсутствует поддержка длительных операций

!!! Также ограничением является то, что посылаемый код должен быть внутри класса с именем Main

 */
@Service
public class CompilerService {

    private static final Logger logger = LoggerFactory.getLogger(CompilerService.class);

    // компилятор
    private final JavaCompiler compiler;

    // время (в секундах), которое дается на выполнение клиентского кода.
    // Если код не выполняется, выбрасывается ошибка (пока что избегаем бесконечных лупов)
    public static final int TIMEOUT = 5; // SECONDS



    public CompilerService(){
        compiler = ToolProvider.getSystemJavaCompiler();
    }


    // компиляция и запуск с использованием временного файла
    public String makeCompilationAndRun(RunRequest request) throws Exception{

        String code = request.getCode();
        Path tempDir = null;
        String filename = "Main.java";

        try {

            // --- Фаза компиляции

            /*
             пишем код во временный java file
             используем безопасную конструкцию try-with-resources
            */
            tempDir = Files.createTempDirectory("java-code-execution"); // создание директории в ../AppData/Local/Temp/
            File sourceFile =new File(tempDir.toFile(), filename); // Создание пустого файла с именем filename
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile))) { // пишем код во временный файл
                writer.write(code);
            }



            // получаем полный путь к созданному временному файлу
            String absPath = sourceFile.getAbsolutePath();



            /* готовим два байт потока - в первый записывается инфа от компилятора в случае успеха
            во второй - лог ошибки
             */
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            // запускаем компиляцию
            int result = compiler.run(null, outputStream, errorStream, absPath);


            // если компиляция закончилась с ошибкой - возвращаем ее лог
            if (result!=0) return errorStream.toString();



            /*

            ---- После компиляции готовимся запускать код

             */



            /*
            создаем новую команду для операционной системы. В конструкторе - название команды и ее аргументы
            - java - команда JVM
            - -cp - classpath флаг, указывающий где искать скомпилированные классы
            - tempDir.toString() - временная директория, куда мы ранее скомпилировали класс
            - название класса, содержащего main() метод
             */
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java", "-cp", tempDir.toString(), "Main"
            );



            // делаем так, чтобы лог ошибки уходил в отдельный поток
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            // готовим два отдельных потока записи для результата выполнения и runtime ошибок
            ByteArrayOutputStream runtimeOutputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream runtimeErrorStream = new ByteArrayOutputStream();

            // готовим потоки выполнения

            // поток обработки успешно выполненного кода
            Thread outputThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                        runtimeOutputStream.write(buffer, 0, bytesRead);
                        /*
                        todo на будущее - читая runtimeOutputStream, можно посылать сообщения последовательно, к примеру через websocket
                         */

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


            // поток обработки ошибки
            Thread errorThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = process.getErrorStream().read(buffer)) != -1) {
                        runtimeErrorStream.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


            // запускаем потоки и, тем самым, начинаем запись логов
            outputThread.start();
            errorThread.start();

            // ждем выполнения процесса, опираясь на заданный параметр времени
            boolean finished = process.waitFor(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS); // 5-second timeout


            /*
            если спустя заданное время процесс не был закончен, принудительно его заканчиваем
            возвращаем то, что успело залететь в лог
             */
            if (!finished) {
                process.destroy(); // Terminate the process

                // тут я присоединяю все то, что попало в консоль до ошибки
                return runtimeOutputStream+  "\n Execution timed out after 5 seconds.";
            }


            /*
            далее делаем проверку - какой код вернул процесс
            В зависимости от значения понимаем, была ли ошибка
             */

            // Проверяем
            int exitCode = process.exitValue();


            // выполнение программы закончилось с ошибкой
            if (exitCode != 0) {

                // main thread ждет окончания выполнения вспомогательных потоков
                outputThread.join();
                errorThread.join();

                // возвращаем ошибку todo  добавь лог программы из runtimeoutputStream, если есть
                String errorMessage = runtimeErrorStream.toString();
                if (errorMessage.isEmpty()) {
                    errorMessage = "Execution failed with exit code: " + exitCode;
                }
                return "Execution failed:\n" + errorMessage;
            }


            // выполнение программы успешно - возвращаем результат, не забыв заджоинить потоки

            outputThread.join();
            errorThread.join();


            return runtimeOutputStream.toString();




        }

        // если при компиляции или запуске произошла какая-то непредвиденная ошибка
        catch (Exception e) {
            return "Error "+e.getMessage();
        }


        // рекурсивно чистим временное хранилище - в независимости от результата выполнения
        finally {


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
