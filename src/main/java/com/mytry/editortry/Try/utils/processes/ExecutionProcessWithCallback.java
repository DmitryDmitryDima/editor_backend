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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ExecutionProcessWithCallback {

    // JDK_SERVER_RUN - not for final dev
    private enum PROCESS_STEP{
        CHECK_DOCKER,
        JAR_COMPILE, JDK_SERVER_RUN,
        DOCKER_IMAGE_BUILD,
        DOCKER_CONTAINER_RUN,
        DOCKER_CONTAINER_REMOVE,
        DOCKER_IMAGE_REMOVE
    }

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
    private AtomicReference<PROCESS_STEP> currentStep = new AtomicReference<>(null);

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

            // todo сценарий убийства зависит от шага
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
                executeStep(PROCESS_STEP.JAR_COMPILE, List.of("mvn.cmd", "package"), true);

                // оповещаем о конце компиляции
                messageCallback.accept(new ExecutionProcessMessageEvent(this, "Successful compilation", projectId,
                        projectDirectory));


                // проверяем наличие docker
                boolean docker = dockerCheck();

                if (docker){
                    String imageName = "javaimage_"+projectId;
                    String containerName = "javacontainer_"+projectId;
                    messageCallback.accept(new ExecutionProcessMessageEvent(this, "-----Prepare virtual env", projectId,
                            projectDirectory));

                    executeStep(PROCESS_STEP.DOCKER_IMAGE_BUILD, List.of("docker", "build",".", "-t", imageName), false);

                    messageCallback.accept(new ExecutionProcessMessageEvent(this, "-----Execute virtual env", projectId,
                            projectDirectory));

                    executeStep(PROCESS_STEP.DOCKER_CONTAINER_RUN, List.of("docker", "run", "--name", containerName, imageName), true);

                    messageCallback.accept(new ExecutionProcessMessageEvent(this, "-----Destroy Venv", projectId,
                            projectDirectory));

                    // подчищаем

                    executeStep(PROCESS_STEP.DOCKER_CONTAINER_REMOVE, List.of("docker", "rm", containerName), false);


                    executeStep(PROCESS_STEP.DOCKER_IMAGE_REMOVE, List.of("docker", "rmi", imageName), false);





                }

                else {
                    // запускаем проект без docker (dev only)
                    executeStep(PROCESS_STEP.JDK_SERVER_RUN, List.of("java", "-jar", "target/fatjar.jar"), true);
                }





                // успешное выполнение всех шагов
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


    // todo - dev only method, оформляем это как полноценный процесс
    private boolean dockerCheck() throws Exception {
        checkInterruption();

        List<String> commandLine = List.of("docker", "--version");
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.redirectErrorStream(true);

        try{
            Process process = processBuilder.start();

            currentProcess.set(process);
            currentStep.set(PROCESS_STEP.CHECK_DOCKER);

            // некоторые системы при отсутствии команды не выбрасывают исключения, а посылают код
            int exitCode = process.waitFor();
            if (exitCode!=0){
                return false;
            }

        }
        catch (Exception e){
            return false;
        }


        return true;






    }


    private void executeStep(PROCESS_STEP step, List<String> commandLine, boolean log) throws Exception{
        checkInterruption();
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(new File(projectDirectory));

        Process process = processBuilder.start();

        currentProcess.set(process);
        currentStep.set(step);

        try (final InputStream stdoutInputStream = process.getInputStream();
             final BufferedReader stdoutReader =
                     new BufferedReader(new InputStreamReader(stdoutInputStream))
        ){
            String out;
            while (running.get() && (out = stdoutReader.readLine()) != null) {
                if (log){
                    messageCallback.accept(new ExecutionProcessMessageEvent(this, out, projectId, projectDirectory));
                }


            }
        }
        catch (Exception e){
            throw new Exception(e.getMessage());
        }

        int exitCode = process.waitFor();
        if (exitCode!=0){
            messageCallback.accept(new ExecutionProcessMessageEvent(this, "fatal error ", projectId, projectDirectory));
            // меняем флаг, прерываем поток
            stop();
        }

        // сбрасываем процесс - тут или следом по цепочке?
        currentProcess.set(null);




    }






}
