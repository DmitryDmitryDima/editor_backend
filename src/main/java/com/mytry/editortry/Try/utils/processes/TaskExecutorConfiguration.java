package com.mytry.editortry.Try.utils.processes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class TaskExecutorConfiguration {

    @Bean(name = "projectExecutor")
    public TaskExecutor threadPoolTaskExecutor() {

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(30);
        taskExecutor.setMaxPoolSize(40);
        taskExecutor.setQueueCapacity(10);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(30);
        taskExecutor.setThreadNamePrefix("project-exec-");
        taskExecutor.initialize();

        return taskExecutor;
    }
}

