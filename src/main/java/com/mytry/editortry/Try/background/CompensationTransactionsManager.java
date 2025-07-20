package com.mytry.editortry.Try.background;

import com.mytry.editortry.Try.model.state.FileDeletingSagaStep;
import com.mytry.editortry.Try.repository.saga.FileDeletingCompensationTransactionRepository;
import com.mytry.editortry.Try.repository.saga.FileIdempotentProcessRepository;
import com.mytry.editortry.Try.service.project.FileDeletingSaga;
import com.mytry.editortry.Try.utils.websocket.WebSocketLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CompensationTransactionsManager {

    @Autowired
    private FileDeletingCompensationTransactionRepository fileDeletingCompensationTransactionRepository;

    @Autowired
    private FileIdempotentProcessRepository fileIdempotentProcessRepository;

    @Autowired
    private FileDeletingSaga fileDeletingSaga;



    // logging
    @Autowired
    private WebSocketLogger logger;


    // читаем базу, обрабатываем зависшие процессы каждые 30 секунд
    @Scheduled(fixedRate = 30000)
    public void fileDeletingTransactionsManagement(){
        //logger.log("запущена фоновая операция выполнения компенсационных транзакций");

        fileDeletingCompensationTransactionRepository.findByAttemptsLessThan(5).forEach(transaction->{
            System.out.println(transaction.getStep());
            if (transaction.getStep().equals(FileDeletingSagaStep.DB_PROCESS_DELETE.name())){

                fileIdempotentProcessRepository.deleteById(transaction.getId());
                // подчищаем транзакцию
                fileDeletingCompensationTransactionRepository.delete(transaction);


            }
        });
    }

}
