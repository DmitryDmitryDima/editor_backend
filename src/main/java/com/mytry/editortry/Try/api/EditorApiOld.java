package com.mytry.editortry.Try.api;



import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionAnswer;
import com.mytry.editortry.Try.dto.dotsuggestion.DotSuggestionRequest;
import com.mytry.editortry.Try.dto.importsuggestion.ImportAnswer;
import com.mytry.editortry.Try.dto.importsuggestion.ImportRequest;
import com.mytry.editortry.Try.dto.run.RunAnswer;
import com.mytry.editortry.Try.dto.run.RunRequest;
import com.mytry.editortry.Try.service.CompilerService;
import com.mytry.editortry.Try.service.AIService;
import com.mytry.editortry.Try.service.codeanalyzis.ParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/editor/")
public class EditorApiOld {

    private static final Logger logger = LoggerFactory.getLogger(EditorApiOld.class);


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


        RunAnswer compilerAnswer = compilerService.makeCompilationAndRun(request);





        return compilerAnswer.optimize(request.getScreenWidth());
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
