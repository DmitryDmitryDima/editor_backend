package com.mytry.editortry.Try.background;


import com.mytry.editortry.Try.utils.websocket.raw.WebSocketLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackgroundTask {





    // logging
    @Autowired
    private WebSocketLogger logger;


    // читаем базу, обрабатываем зависшие процессы каждые 30 секунд
    @Scheduled(fixedRate = 5000)
    public void fileDeletingTransactionsManagement(){

        logger.log("background task");




    }

}
