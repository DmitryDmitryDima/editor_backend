package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.dashboard.DashboardAnswerFileDeleting;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
создаю визуальную репрезентацию некоторых шагов, в частности жизненного цикла саги
 */
@RestController
@RequestMapping("/test/")
public class DashboardAPI {

    @GetMapping("/delete/files")
    public ResponseEntity<DashboardAnswerFileDeleting> deleteFile(){
        return null;
    }
}
