package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.dto.LMRequestData;
import com.mytry.editortry.Try.dto.LMResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


// Общение с языковой моделью
//todo возможно общение в json формате, гляди https://github.com/ollama/ollama/blob/main/docs/api.md
@Service
public class LanguageModelService {

    public static final String API_ADDRESS = "http://127.0.0.1:11434/api/generate";
    public static final String MODEL_NAME = "gemma3:1b"; // lightweight - gemma3:1b

    public static final String IMPORT_PROMPT = """ 

        write only imports for java code below:

""";

    public static final String ERROR_PROMPT = """
            
            check java code below and find errors:
            
            """;



    public LMResponse sendARequest(String prompt){

        // create headers
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json");
        headers.setAll(map);

        // create request data
        LMRequestData lmRequestData = new LMRequestData(prompt, MODEL_NAME, false);



        HttpEntity<LMRequestData> request = new HttpEntity<>(lmRequestData, headers);
        ResponseEntity<LMResponse> response = new RestTemplate().postForEntity(API_ADDRESS, request, LMResponse.class);


        // TODO как извлечь только импорт из строки - т.к. иногда ответ может быть разного формата
        // при обращении к модели - она остается в памяти, или каждый раз загружается по новой??




        return response.getBody();
    }

}
