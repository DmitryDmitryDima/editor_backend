package com.mytry.editortry.Try.service;

import com.mytry.editortry.Try.dto.dashboard.DashboardAnswer;
import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.repository.FileRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {



    @Autowired
    private FileRepository fileRepository;



    public DashboardAnswer fetchAllFileEntities(){

        DashboardAnswer dashboardAnswer = new DashboardAnswer();



        List<File> files = fileRepository.findAll();


        List<FlatTreeMember> flatTreeMembers = new ArrayList<>();

        files.forEach(el->{
            FlatTreeMember flatTreeMember = new FlatTreeMember();
            flatTreeMember.setData(el.getName()+"."+el.getExtension());
            flatTreeMember.setIndex("file_"+el.getId());
            flatTreeMembers.add(flatTreeMember);
        });


        dashboardAnswer.setFiles(flatTreeMembers);


        return dashboardAnswer;


    }


}
