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



    public RunAnswer optimize(Integer width){

        StringBuilder stringBuffer = new StringBuilder();

        int parts = message.length()*8/(width);

        int pointer = 0;
        while (parts>0){
            String sub = message.substring(pointer, pointer+(width/8));
            stringBuffer.append(sub);
            stringBuffer.append("\n");
            pointer+=(width/8);
            parts--;
        }

        String finalSub = message.substring(pointer, message.length()-1);

        stringBuffer.append(finalSub);
        this.message = stringBuffer.toString();
        return this;
    }



}
