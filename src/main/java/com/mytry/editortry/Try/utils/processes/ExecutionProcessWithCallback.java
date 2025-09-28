package com.mytry.editortry.Try.utils.processes;

import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessErrorEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessInterruptionEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessMessageEvent;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ExecutionProcessWithCallback {

    // interruption callback
    private Consumer<ExecutionProcessInterruptionEvent> interruptionCallback;

    // message callback
    private Consumer<ExecutionProcessMessageEvent> messageCallback;

    // error callback
    private Consumer<ExecutionProcessErrorEvent> errorCallback;

    @Getter
    private final Long projectId;

    private AtomicBoolean running = new AtomicBoolean(false);

    private AtomicReference<Thread> workingThread = new AtomicReference<>(null);

    private AtomicReference<Process> currentProcess = new AtomicReference<>(null);

    @Setter
    private String projectDirectory;

    public ExecutionProcessWithCallback(Consumer<ExecutionProcessInterruptionEvent> interruptionCallback,
                                        Consumer<ExecutionProcessMessageEvent> messageCallback,
                                        Consumer<ExecutionProcessErrorEvent> errorCallback,
                                        Long projectId) {
        this.interruptionCallback = interruptionCallback;
        this.messageCallback = messageCallback;
        this.errorCallback = errorCallback;
        this.projectId = projectId;
    }


    // проверка на прерванность потока юзером
    private void checkInterruption() throws InterruptedException{
        // процесс мертв
        if (currentProcess.get()!=null && !currentProcess.get().isAlive()){
            throw new InterruptedException("process was stopped");
        }
        // запуск вручную остановлен пользователем
        if (!running.get()){
            throw new InterruptedException("project was stopped");
        }
        if (Thread.currentThread().isInterrupted()){
            throw new InterruptedException("thread was interrupted");
        }


    }

    // метод для остановки потока во время исполнения - вызывается пользователем с фронта или потоком в случае ошибки внешнего процесса
    public void stop(){
        if (running.compareAndSet(true, false)){

            // принудительно убиваем текущий внешний процесс
            Process process = currentProcess.get();
            if (process!=null){
                process.destroy();
                if (process.isAlive()){
                    process.destroyForcibly();
                }
            }

            // прерываем сам поток
            if (workingThread.get()!=null){
                workingThread.get().interrupt();
            }
        }
    }




    public void start() {


        if (running.compareAndSet(false, true)){
            // сохраняем экземпляр потока для его ручного контроля со стороны внешнего потока
            workingThread.set(Thread.currentThread());
            System.out.println(Thread.currentThread().getName()+ " working thread");


            try {





                // создаем jar
                createFatJar();

                // оповещаем о конце компиляции
                messageCallback.accept(new ExecutionProcessMessageEvent(this, "Successful compilation", projectId,
                        projectDirectory));

                // запускаем проект
                runJar();


                messageCallback.accept(new ExecutionProcessMessageEvent(this, "Execution completed", projectId,
                        projectDirectory));
            }
            catch (Exception exception){
                errorCallback.accept(new ExecutionProcessErrorEvent(this,
                        "Error!: "+exception.getMessage(), projectId, projectDirectory));

            }
            finally {

                // внутренний ивент остановки - последний в ходе выполнения процесса
                ExecutionProcessInterruptionEvent interruptionEvent = new ExecutionProcessInterruptionEvent(this,
                        projectId, ExecutionProcessInterruptionEvent.InterruptionType.Internal);
                interruptionEvent.setDirectory(projectDirectory);

                interruptionCallback.accept(interruptionEvent);
            }


        }




    }

    // запускаем jar (временная реализация с прямым запуском jvm
    private void runJar() throws Exception{

        checkInterruption();



        ProcessBuilder jarProcessBuilder = new ProcessBuilder("java", "-jar", "target/fatjar.jar");
        jarProcessBuilder.redirectErrorStream(true); // объединяем поток ошибок и поток стандартного лога
        jarProcessBuilder.directory(new File(projectDirectory));

        Process process = jarProcessBuilder.start();
        currentProcess.set(process);

        try (final InputStream stdoutInputStream = process.getInputStream();
             final BufferedReader stdoutReader =
                     new BufferedReader(new InputStreamReader(stdoutInputStream))
        ){
            String out;
            while (running.get() && (out = stdoutReader.readLine()) != null) {

                messageCallback.accept(new ExecutionProcessMessageEvent(this, out, projectId, projectDirectory));
            }
        }
        catch (Exception e){
            throw new Exception(e.getMessage());
        }

        int exitCode = process.waitFor();
        if (exitCode!=0){
            messageCallback.accept(new ExecutionProcessMessageEvent(this, "fatal error", projectId, projectDirectory));
            // меняем флаг, прерываем поток
            stop();
        }

        // сбрасываем процесс
        currentProcess.set(null);

    }


    // создаем исполняемый jar файл со всеми зависимостями
    private void createFatJar() throws Exception{

        checkInterruption();






        ProcessBuilder jarProcessBuilder = new ProcessBuilder("mvn.cmd", "package");
        jarProcessBuilder.redirectErrorStream(true); // объединяем поток ошибок и поток стандартного лога
        jarProcessBuilder.directory(new File(projectDirectory));
        Process process = jarProcessBuilder.start();
        currentProcess.set(process);


        try (final InputStream stdoutInputStream = process.getInputStream();
             final BufferedReader stdoutReader =
                     new BufferedReader(new InputStreamReader(stdoutInputStream))
        ){
            String out;
            while (running.get() && (out = stdoutReader.readLine()) != null) {

                //messageCallback.accept(new ExecutionProcessMessageEvent(this, out, projectId, projectDirectory));
            }
        }
        catch (Exception e){
            throw new Exception(e.getMessage());
        }

        int exitCode = process.waitFor();
        // процесс завершился с ошибкой - остановка всего
        if (exitCode!=0){
            messageCallback.accept(new ExecutionProcessMessageEvent(this, "fatal error", projectId, projectDirectory));
            // меняем флаг, прерываем поток
            stop();
        }


        // сбрасываем процесс
        currentProcess.set(null);










    }




}
