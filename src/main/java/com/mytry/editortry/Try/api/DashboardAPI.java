package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.dashboard.DashboardAnswer;
import com.mytry.editortry.Try.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
создаю визуальную репрезентацию некоторых шагов, в частности жизненного цикла саги
 */
@RestController
@RequestMapping("/api/test/")
public class DashboardAPI {

    @Autowired
    private DashboardService dashboardService;


    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAnswer> statistics(){




        return ResponseEntity.ok(dashboardService.fetchAllFileEntities());
    }



}
