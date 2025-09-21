package com.mytry.editortry.Try.utils.processes;

import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessErrorEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessInterruptionEvent;
import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessMessageEvent;
import com.mytry.editortry.Try.utils.processes.events.ProcessEventType;
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
        if (!running.get()){
            throw new InterruptedException("project was stopped");
        }
        if (Thread.currentThread().isInterrupted()){
            throw new InterruptedException("thread was interrupted");
        }
    }

    // метод для остановки потока "снаружи"
    public void stop(){
        if (running.compareAndSet(true, false)){
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






                createFatJar();

                messageCallback.accept(new ExecutionProcessMessageEvent(this, "Success:end of execution", projectId));
            }
            catch (Exception exception){
                errorCallback.accept(new ExecutionProcessErrorEvent(this,
                        "Error!: "+exception.getMessage(), projectId));

            }
            finally {

                interruptionCallback.accept(new ExecutionProcessInterruptionEvent(this,
                        projectId,
                        ExecutionProcessInterruptionEvent
                        .InterruptionType.Internal));
            }


        }




    }


    // создаем исполняемый jar файл со всеми зависимостями
    private void createFatJar() throws Exception{

        checkInterruption();

        //Thread.sleep(10000);



        // todo процесс нужно вынести в AtomicReference
        ProcessBuilder jarProcessBuilder = new ProcessBuilder("mvn.cmd", "package");
        jarProcessBuilder.directory(new File(projectDirectory));
        Process process = jarProcessBuilder.start();


        try (final InputStream stdoutInputStream = process.getInputStream();
             final BufferedReader stdoutReader =
                     new BufferedReader(new InputStreamReader(stdoutInputStream))
        ){
            String out;
            while (running.get() && (out = stdoutReader.readLine()) != null) {

                messageCallback.accept(new ExecutionProcessMessageEvent(this, out, projectId));
            }
        }
        catch (Exception e){
            throw new Exception(e.getMessage());
        }









    }




}
