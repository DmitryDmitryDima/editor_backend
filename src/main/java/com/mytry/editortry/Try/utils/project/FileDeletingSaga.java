package com.mytry.editortry.Try.utils.project;


import com.mytry.editortry.Try.dto.saga.FileDeletingInfo;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.saga.FileDeletingCompensationTransaction;
import com.mytry.editortry.Try.model.state.FileDeletingSagaStep;
import com.mytry.editortry.Try.model.state.FileStatus;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.saga.FileDeletingCompensationTransactionRepository;
import com.mytry.editortry.Try.repository.saga.FileIdempotentProcessRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;




// ООП репрезентация для саги удаления файла - компонент связан с сервисом
@Component
public class FileDeletingSaga {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileDeletingCompensationTransactionRepository fileDeletingCompensationTransactionRepository;

    @Autowired
    private FileIdempotentProcessRepository fileIdempotentProcessRepository;



   // точка входа в сагу - ошибка в данном методе не запускает цепочки вниз

   // метод set уже меняет состояние ! - > особенность работы hibernate, который хранит все в памяти
    @Transactional(rollbackOn = Exception.class)
   public void db_status_change(File file) throws Exception{




       file.setStatus(FileStatus.DELETING);

       try {


           //throw new RuntimeException("suka");
           fileRepository.save(file);

       }
       catch (RuntimeException runtimeException){


           try {

               // освобождаем файл в отдельной транзакции
               delete_file_idempotent_process(file.getId());

           }
           catch (RuntimeException e){
               idempotent_process_create_compensation(file.getId());
           }

           throw new IllegalArgumentException("now we can't remove this file. Try later");
       }

   }

   // вызов в пределах transactional создаст отдельную транзакцию
   @Transactional(Transactional.TxType.REQUIRES_NEW)
   private void delete_file_idempotent_process(long id){
        fileIdempotentProcessRepository.deleteById(id);
   }

   @Transactional(Transactional.TxType.REQUIRES_NEW)
   private void idempotent_process_create_compensation(long id){
       // оч редкий сценарий - если отвалилась база процессов
       FileDeletingCompensationTransaction fileDeletingCompensationTransaction = new FileDeletingCompensationTransaction();
       fileDeletingCompensationTransaction.setFile_id(id);
       fileDeletingCompensationTransaction.setStep(FileDeletingSagaStep.DB_STATUS_CHANGE.name());
       fileDeletingCompensationTransactionRepository.save(fileDeletingCompensationTransaction);

       // todo подумай, что будет, если отвалится база компенсаций
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
