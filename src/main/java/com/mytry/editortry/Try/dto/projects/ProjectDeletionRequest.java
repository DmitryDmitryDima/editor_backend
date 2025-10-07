package com.mytry.editortry.Try.dto.projects;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDeletionRequest {

    // задел на будущее - пользователь сможет либо переместить проект в корзину, либо удалить сразу
    private Boolean needBasket;

    private Long projectId;


}
