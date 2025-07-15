package com.mytry.editortry.Try.dto.dashboard;

import com.mytry.editortry.Try.model.saga.FileDeletingCompensationTransaction;
import com.mytry.editortry.Try.model.saga.FileIdempotentProcess;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnswerFileDeleting {

    private List<FileDeletingCompensationTransaction> fileDeletingCompensationTransactions;

    private List<FileIdempotentProcess> fileIdempotentProcesses;

}
