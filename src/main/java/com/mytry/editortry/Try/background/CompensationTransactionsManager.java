package com.mytry.editortry.Try.background;

import com.mytry.editortry.Try.model.state.FileDeletingSagaStep;
import com.mytry.editortry.Try.repository.saga.FileDeletingCompensationTransactionRepository;
import com.mytry.editortry.Try.utils.project.FileDeletingSaga;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CompensationTransactionsManager {

    @Autowired
    private FileDeletingCompensationTransactionRepository fileDeletingCompensationTransactionRepository;

    @Autowired
    private FileDeletingSaga fileDeletingSaga;

    // читаем базу, обрабатываем зависшие процессы каждые 30 секунд
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void fileDeletingTransactionsManagement(){

        fileDeletingCompensationTransactionRepository.findByAttemptsLessThan(5).forEach(transaction->{
            if (transaction.getStep().equals(FileDeletingSagaStep.DB_STATUS_CHANGE.name())){
                System.out.println("delete process");

            }
        });
    }

}
