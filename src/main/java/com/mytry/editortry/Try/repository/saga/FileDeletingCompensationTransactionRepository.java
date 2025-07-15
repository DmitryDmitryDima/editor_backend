package com.mytry.editortry.Try.repository.saga;


import com.mytry.editortry.Try.model.saga.FileDeletingCompensationTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileDeletingCompensationTransactionRepository
        extends JpaRepository<FileDeletingCompensationTransaction, Long> {

    public List<FileDeletingCompensationTransaction> findByAttemptsLessThan(long attempts);

}
