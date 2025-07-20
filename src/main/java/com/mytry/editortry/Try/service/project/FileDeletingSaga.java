package com.mytry.editortry.Try.service.project;


import com.mytry.editortry.Try.dto.saga.FileDeletingInfo;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.saga.FileDeletingCompensationTransaction;
import com.mytry.editortry.Try.model.state.FileDeletingSagaStep;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.saga.FileDeletingCompensationTransactionRepository;
import com.mytry.editortry.Try.repository.saga.FileIdempotentProcessRepository;
import com.mytry.editortry.Try.utils.websocket.WebSocketLogger;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;




// ООП репрезентация для саги удаления файла

/*
TODO переписываем сагу так, чтобы каждый шаг содержал в себе только одну бд операцию !!!
Создаем специальный сервис, принимающий заявки на сохранение компенсаций - CompensationTransactionsAssistant
Данный сервис принимает заявку на сохранение компенсации, после чего  записывет ее в таблицу.
Если происходит ошибка - ждет, пока оживет база данных и повторяет вновь. При достижении некоторого числа попыток пишет лог в файл
Сохраненная транзакция обрабатывается фоново CompensationTransactionManager, который по номеру шагу восстанавливает цепочку саги
Если происходит ошибка в менеджере, то посылается заявка (event) на обновление числа попыток в транзакционной записи
Если ошибки нет - посылается заявка на удаление транзакции из бд.
ТАКИМ ОБРАЗОМ - ОСНОВНОЙ ПРИНЦИП - АТОМАРНОСТЬ ВЕЗДЕ
 */
@Service
public class FileDeletingSaga {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileDeletingCompensationTransactionRepository fileDeletingCompensationTransactionRepository;

    @Autowired
    private FileIdempotentProcessRepository fileIdempotentProcessRepository;


    // логи
    @Autowired
    private WebSocketLogger webSocketLogger;



   // точка входа в сагу - ошибка в данном методе не запускает цепочки вниз
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void saga_start_db_status_change(File file) throws Exception{
        throw new IllegalArgumentException("what a fuck");
        //file.setStatus(FileStatus.DELETING);
        //fileRepository.save(file);

    }






   public void disk_rename(FileDeletingInfo info){

       // создаем ключ переименования
       String uuid = UUID.randomUUID().toString();



       java.io.File file = new java.io.File(info.getPath());
       String deletePath = info.getPath()+"."+uuid+".delete";
       try {
           boolean result = file.renameTo(new java.io.File(deletePath));

           if (result) {
               // ! внимание - бросаем новый путь в случае успеха
               info.setPath(deletePath);
               db_delete(info);
           }
           else {
               throw new IllegalArgumentException("doesn't rename");
           }
       }
       catch (Exception e){
           FileDeletingCompensationTransaction fileDeletingCompensationTransaction = info
                   .buildTransaction(FileDeletingSagaStep.DISK_RENAME);
           fileDeletingCompensationTransactionRepository.save(fileDeletingCompensationTransaction);
       }


       }





   public void db_delete(FileDeletingInfo info) {

       try {
           fileRepository.deleteById(info.getId());
           disk_delete(info);
       }
       catch (Exception e){
           FileDeletingCompensationTransaction fileDeletingCompensationTransaction = info
                   .buildTransaction(FileDeletingSagaStep.DB_DELETE);
           fileDeletingCompensationTransactionRepository.save(fileDeletingCompensationTransaction);
       }

   }

   public void disk_delete(FileDeletingInfo info){

       try {
           Files.delete(Paths.get(info.getPath()));

           // todo тестируем зачистку процесса - работает
           db_process_delete(info);
       }
       catch (Exception e){
           FileDeletingCompensationTransaction fileDeletingCompensationTransaction =  info
                   .buildTransaction(FileDeletingSagaStep.DISK_DELETE);
           fileDeletingCompensationTransactionRepository.save(fileDeletingCompensationTransaction);
       }

   }

   @Transactional(Transactional.TxType.REQUIRES_NEW)
   public void db_process_delete(FileDeletingInfo info){
        try {
            fileIdempotentProcessRepository.deleteById(info.getId());
        }
        catch (Exception e){
            FileDeletingCompensationTransaction fileDeletingCompensationTransaction = info
                    .buildTransaction(FileDeletingSagaStep.DB_PROCESS_DELETE);
            fileDeletingCompensationTransactionRepository.save(fileDeletingCompensationTransaction);
        }
   }

}
