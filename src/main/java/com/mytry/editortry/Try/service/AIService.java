package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.lm.LMRequestWithPrompt;
import com.mytry.editortry.Try.dto.lm.LMResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/*

В этом сервисе реализуется общение с языковой моделью
TODO некоторые параметры целесообразно вынести в файл - с загрузкой в переменную
возможно общение в json формате, гляди https://github.com/ollama/ollama/blob/main/docs/api.md
 */

@Service
public class AIService {

    //адрес api нейросети
    public static final String API_ADDRESS = "http://127.0.0.1:11434/api/generate";

    // текущая модель
    public static final String MODEL_NAME = "gemma3:1b"; // lightweight - gemma3:1b

    // prompt - оптимальный, протестированный текст запроса для цели извлечь импорт
    public static final String IMPORT_PROMPT = """ 

        write only imports for java code below:

""";







    // utils



    // метод обращения к серверу нейросети
    public LMResponse sendARequest(String prompt){

        // create headers
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json");
        headers.setAll(map);

        // create request data with prompt
        LMRequestWithPrompt lmRequestWithPrompt = new LMRequestWithPrompt(prompt, MODEL_NAME, false);


        // собираем запрос, отправляем его, получаем ответ
        HttpEntity<LMRequestWithPrompt> request = new HttpEntity<>(lmRequestWithPrompt, headers);
        ResponseEntity<LMResponse> response = new RestTemplate().postForEntity(API_ADDRESS, request, LMResponse.class);


        // TODO как извлечь только импорт из строки - т.к. иногда ответ может быть разного формата
        // при обращении к модели - она остается в памяти, или каждый раз загружается по новой??




        return response.getBody();
    }


    // метод, гарантированно извлекающий строку с импортом из ответа нейросети
    private Set<String> extractImport(String lmResponse){


        String[] splitted = lmResponse.split("\\r?\\n");

        return Arrays.stream(splitted)
                .filter(el->el.startsWith("import"))
                .collect(Collectors.toSet());
    }







}
