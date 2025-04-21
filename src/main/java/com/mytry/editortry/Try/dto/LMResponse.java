package com.mytry.editortry.Try.dto;


import lombok.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LMResponse {

    private String response;
    private String created_at;

    // пытаемся извлечь конкретно строку с импортом
    public Set<String> importOptimize(){


        String[] splitted = response.split("\\r?\\n");



        return Arrays.stream(splitted)
                .filter(el->el.startsWith("import"))
                .collect(Collectors.toSet());
    }



}
