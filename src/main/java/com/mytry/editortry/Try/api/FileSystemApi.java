package com.mytry.editortry.Try.api;

import com.mytry.editortry.Try.dto.FileTextResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files/")
public class FileSystemApi {


    // в будущем это значение будет грузиться из базы данных
    @GetMapping("/initial/")
    public FileTextResponse initialEditorValue(){

        String initialText = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\");\n" +
                "    }\n" +
                "}";

        return new FileTextResponse(initialText);
    }
}
