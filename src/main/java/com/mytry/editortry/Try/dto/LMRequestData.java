package com.mytry.editortry.Try.dto;


import lombok.*;


@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LMRequestData {

    private String prompt;

    private String model;

    private boolean stream;

}
