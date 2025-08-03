package com.mytry.editortry.Try.service;

import com.mytry.editortry.Try.dto.dashboard.DashboardAnswer;
import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.repository.FileRepository;

import com.mytry.editortry.Try.utils.cache.CacheSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {



    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private CacheSystem cacheSystem;



    public DashboardAnswer fetchAllFileEntities(){

        DashboardAnswer dashboardAnswer = new DashboardAnswer();


        Pageable firstPageWithTwoElements = PageRequest.of(0, 2);
        Page<File> files = fileRepository.findAll(firstPageWithTwoElements);


        List<FlatTreeMember> flatTreeMembers = new ArrayList<>();

        files.forEach(el->{
            FlatTreeMember flatTreeMember = new FlatTreeMember();
            flatTreeMember.setData(el.getName()+"."+el.getExtension());
            flatTreeMember.setIndex("file_"+el.getId());
            flatTreeMembers.add(flatTreeMember);
        });


        dashboardAnswer.setFiles(flatTreeMembers);
        dashboardAnswer.setCache(cacheSystem.getProjectsCaches());


        return dashboardAnswer;


    }


}
