package com.mytry.editortry.Try.dto.run;


import lombok.*;

// Информация, передаваемая в виде json от фронта при нажатии кнопки run
// @Value annotation means that this dto will be immutable

@Value
public class RunRequest {

    String code;
    Integer screenWidth;


}
