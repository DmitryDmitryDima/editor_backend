package com.mytry.editortry.Try.utils.processes;

import com.mytry.editortry.Try.utils.processes.events.ExecutionProcessEvent;
import com.mytry.editortry.Try.utils.processes.events.ProcessEventType;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// процесс генерирует события в отдельном потоке и отправляет их на хост
// необходима реализация прерывания
public class ExecutionProcess {

    private ProjectExecutionStateManager state;

    private final Long projectId;

    private AtomicBoolean running = new AtomicBoolean(false);

    private AtomicReference<Thread> workingThread = new AtomicReference<>(null);
    private String projectDirectory;

    public ExecutionProcess(ProjectExecutionStateManager state, Long projectId){
        this.state = state;
        this.projectId = projectId;
    }


    // создаем исполняемый jar файл со всеми зависимостями
    private void createFatJar() throws Exception{

        checkInterruption();




        ProcessBuilder jarProcessBuilder = new ProcessBuilder("mvn.cmd", "package");
        jarProcessBuilder.directory(new File(projectDirectory));
        Process process = jarProcessBuilder.start();

        try (final InputStream stdoutInputStream = process.getInputStream();
             final BufferedReader stdoutReader =
                     new BufferedReader(new InputStreamReader(stdoutInputStream))
        ){
            String out;
            while (running.get() && (out = stdoutReader.readLine()) != null) {
                //
                state.onEvent(generateExecutionEvent(ProcessEventType.PROCESS_INFO, out));
            }
        }
        catch (Exception e){
            throw new Exception(e.getMessage());
        }









    }

    private void checkInterruption() throws InterruptedException{
        if (!running.get()){
            throw new InterruptedException("project was stopped");
        }
        if (Thread.currentThread().isInterrupted()){
            throw new InterruptedException("thread was interrupted");
        }
    }




    public void start(){


        if (running.compareAndSet(false, true)){
            // сохраняем экземпляр потока для его ручного контроля со стороны внешнего потока
            workingThread.set(Thread.currentThread());
            System.out.println(Thread.currentThread().getName()+ " working thread");


            try {

                // первый шаг - проверка условий
                projectDirectory = state.onStart(projectId);
                System.out.println(projectDirectory);




                createFatJar();
                // успешное завершение
                state.onEvent(generateExecutionEvent(ProcessEventType.PROCESS_INFO,
                        "execution end"));
            }
            catch (Exception exception){
                state.onEvent(generateExecutionEvent(ProcessEventType.PROCESS_ERROR,
                        "execution interrupted with message "+exception.getMessage()));
            }
            finally {

                state.onEvent(generateExecutionEvent(ProcessEventType.PROCESS_END,
                        "process terminated"));
            }


        }




    }

    public void stop(){
        if (running.compareAndSet(true, false)){
            if (workingThread.get()!=null){
                workingThread.get().interrupt();
            }
        }
    }

    private ExecutionProcessEvent generateExecutionEvent(ProcessEventType type, String message){
        ExecutionProcessEvent event = new ExecutionProcessEvent();
        event.setType(type);
        event.setMessage(message);
        event.setProjectId(projectId);
        return event;
    }





}

