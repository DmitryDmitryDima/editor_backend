package com.mytry.editortry.Try.api;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/editor")
public class PagesController {



    @GetMapping()
    public String code(){
        return "codeeditor";
    }


}
