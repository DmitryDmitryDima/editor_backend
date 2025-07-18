package com.mytry.editortry.Try.service;

import com.mytry.editortry.Try.dto.dashboard.DashboardAnswerFileDeletingSaga;
import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.saga.FileDeletingCompensationTransaction;
import com.mytry.editortry.Try.model.saga.FileIdempotentProcess;
import com.mytry.editortry.Try.model.state.FileStatus;
import com.mytry.editortry.Try.repository.FileRepository;
import com.mytry.editortry.Try.repository.saga.FileDeletingCompensationTransactionRepository;
import com.mytry.editortry.Try.repository.saga.FileIdempotentProcessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private FileDeletingCompensationTransactionRepository fileDeletingCompensationTransactionRepository;

    @Autowired
    private FileIdempotentProcessRepository fileIdempotentProcessRepository;

    @Autowired
    private FileRepository fileRepository;



    public DashboardAnswerFileDeletingSaga fetchAllFileDeletingEntities(){

        DashboardAnswerFileDeletingSaga dashboardAnswerFileDeletingSaga = new DashboardAnswerFileDeletingSaga();

        List<FileDeletingCompensationTransaction> transactions = fileDeletingCompensationTransactionRepository.findAll();

        List<FileIdempotentProcess> processes = fileIdempotentProcessRepository.findAll();

        List<File> files = fileRepository.findByStatus(FileStatus.DELETING);


        List<FlatTreeMember> flatTreeMembers = new ArrayList<>();

        files.forEach(el->{
            FlatTreeMember flatTreeMember = new FlatTreeMember();
            flatTreeMember.setData(el.getName()+"."+el.getExtension());
            flatTreeMember.setIndex("file_"+el.getId());
            flatTreeMembers.add(flatTreeMember);
        });


        dashboardAnswerFileDeletingSaga.setFilesToDelete(flatTreeMembers);
        dashboardAnswerFileDeletingSaga.setFileIdempotentProcesses(processes);
        dashboardAnswerFileDeletingSaga.setFileDeletingCompensationTransactions(transactions);


        return dashboardAnswerFileDeletingSaga;


    }


}
