package com.mytry.editortry.Try.service;

import com.mytry.editortry.Try.model.saga.FileIdempotentProcess;
import com.mytry.editortry.Try.repository.saga.FileIdempotentProcessRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
отдельный сервис гарантирует, что hibernate сможет сделать действие отдельной транзакцией
 */
@Service
public class FileIdempotentProcessService {

    @Autowired
    private FileIdempotentProcessRepository fileIdempotentProcessRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void deleteProcess(long id){
        fileIdempotentProcessRepository.deleteById(id);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void createProcess(FileIdempotentProcess fileIdempotentProcess){
        fileIdempotentProcessRepository.save(fileIdempotentProcess);
    }
}
