package com.mytry.editortry.Try.api;



import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import com.mytry.editortry.Try.dto.importsuggestion.ImportAnswer;
import com.mytry.editortry.Try.dto.importsuggestion.ImportRequest;
import com.mytry.editortry.Try.dto.run.RunAnswer;
import com.mytry.editortry.Try.dto.run.RunRequest;
import com.mytry.editortry.Try.service.CompilerService;
import com.mytry.editortry.Try.service.AIService;
import com.mytry.editortry.Try.service.ParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/editor/")
public class EditorApi {

    private static final Logger logger = LoggerFactory.getLogger(EditorApi.class);


    // сервис, отвечающий за все, связанное с запуском кода
    @Autowired
    private CompilerService compilerService;

    // сервис, отвечающий за все, относящееся к различного рода подсказкам
    @Autowired
    private ParserService parserService;

    // сервис, отвечающий за общение с нейросетью/нейросетями
    @Autowired
    private AIService AIService;







    // запуск кода - его компиляция + вывод в консоль результата (в перспективе - динамичен)
    @PostMapping("/run")
    public RunAnswer run(@RequestBody RunRequest request) throws Exception {

        // форматирование строки закинь в сервис - 8px на символ
        String compilerAnswer = compilerService.makeCompilationAndRun(request);
        Integer width = request.getScreenWidth();
        StringBuilder stringBuffer = new StringBuilder();

        int parts = compilerAnswer.length()*8/(width);

        int pointer = 0;
        while (parts>0){
            String sub = compilerAnswer.substring(pointer, pointer+(width/8));
            stringBuffer.append(sub);
            stringBuffer.append("\n");
            pointer+=(width/8);
            parts--;
        }

        String finalSub = compilerAnswer.substring(pointer, compilerAnswer.length()-1);

        stringBuffer.append(finalSub);




        return new RunAnswer(stringBuffer.toString());
    }




    // запрос происходит при нажатии кнопки импорта - происходит обращение к нейросети

    @PostMapping("/suggest/import")
    public ImportAnswer suggestImport(@RequestBody ImportRequest importRequest){




       return parserService.importSuggestion(importRequest);
    }







    // фронтенд фиксирует событие и посылает запрос на анализ - поставлена точка
    @PostMapping("/suggest/dot")
    public DotSuggestionAnswer dot(@RequestBody DotSuggestionRequest request ) {

        return parserService.dotSuggestion(request);
    }


    // фронтенд фиксирует событие и посылает запрос на анализ - пользователь начал что-то писать
    @PostMapping("/suggest/word")
    public void word(){
        //TODO тут мы обращаемся к парсеру, загружаем доступные в коде имена переменных, совпадающие с уже введенной частью
        // возможно сделать что-то вроде кеша
    }










}
