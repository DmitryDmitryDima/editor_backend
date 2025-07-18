package com.mytry.editortry.Try.dto.dashboard;

import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import com.mytry.editortry.Try.model.saga.FileDeletingCompensationTransaction;
import com.mytry.editortry.Try.model.saga.FileIdempotentProcess;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnswerFileDeletingSaga {

    private List<FileDeletingCompensationTransaction> fileDeletingCompensationTransactions = new ArrayList<>();

    private List<FileIdempotentProcess> fileIdempotentProcesses = new ArrayList<>();

    private List<FlatTreeMember> filesToDelete = new ArrayList<>();

}
