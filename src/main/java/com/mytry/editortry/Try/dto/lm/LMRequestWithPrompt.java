package com.mytry.editortry.Try.dto.lm;


import lombok.*;


@Value
public class LMRequestWithPrompt {

    String prompt;

    String model;

    boolean stream;

}
