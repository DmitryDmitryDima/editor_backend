package com.mytry.editortry.Try.dto.run;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

// Информация, передаваемая после запуска кода - то, что на фронте отобразится в консоли
// как идея - мы можем посылать тип сообщения - ошибка или успех, в зависимости от этого фронт будет думать над оформлением
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunAnswer {

    // по сути мы возвращаем message - оставим такое имя
    String message;



}
