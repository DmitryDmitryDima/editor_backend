package com.mytry.editortry.Try.background;

import com.mytry.editortry.Try.model.state.FileDeletingSagaStep;
import com.mytry.editortry.Try.repository.saga.FileDeletingCompensationTransactionRepository;
import com.mytry.editortry.Try.repository.saga.FileIdempotentProcessRepository;
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
    private FileIdempotentProcessRepository fileIdempotentProcessRepository;

    @Autowired
    private FileDeletingSaga fileDeletingSaga;


    // читаем базу, обрабатываем зависшие процессы каждые 30 секунд
    @Scheduled(fixedRate = 30000)
    public void fileDeletingTransactionsManagement(){

        fileDeletingCompensationTransactionRepository.findByAttemptsLessThan(5).forEach(transaction->{
            System.out.println(transaction.getStep());
            if (transaction.getStep().equals(FileDeletingSagaStep.DB_STATUS_CHANGE.name())){
                System.out.println("delete process");
                try {
                    fileIdempotentProcessRepository.deleteById(transaction.getFile_id());
                    System.out.println("ПРОЦЕСС УДАЛЕН для "+transaction.getFile_id());
                }
                catch (Exception e){
                    transaction.setAttempts(transaction.getAttempts()+1);
                    fileDeletingCompensationTransactionRepository.save(transaction);
                }


            }
        });
    }

}
