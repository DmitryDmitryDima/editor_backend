package com.mytry.editortry.Try.dto.saga;


import com.mytry.editortry.Try.model.saga.FileDeletingCompensationTransaction;
import com.mytry.editortry.Try.model.state.FileDeletingSagaStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
информация, необходимая для FileDeletingSaga
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDeletingInfo {

    private String path;

    private Long id;



    public FileDeletingCompensationTransaction buildTransaction(FileDeletingSagaStep step){
        FileDeletingCompensationTransaction transaction = new FileDeletingCompensationTransaction();
        transaction.setStep(step.name());
        transaction.setFile_id(id);
        transaction.setDiskPath(path);
        return transaction;
    }


}
