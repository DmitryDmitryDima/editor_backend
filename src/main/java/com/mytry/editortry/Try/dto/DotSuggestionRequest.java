package com.mytry.editortry.Try.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DotSuggestionRequest {

    // контекст
    private String code;
    // объект
    private String object;


    private Integer absolute;

    private String line;






    // зная код, идущий до точки, а также позицию курсора, мы можем, к примеру, закомментить переменную
    // если точка ставится после метода, мы добавляем ;
    public String makeCodeComplete() throws Exception{

        // соответственно на старте проверяем, стоит ли скобка перед точкой, если да - то имеем дело с вызовом метода



        int index = absolute;

        // дальше в зависимости от типа случая ....

        // Реализация для переменной - курсор стоит после точки, а точка после имени переменной - длина объекта плюс точки
        int objectLength = object.length()+1;
        String editedCode = code.substring(0, index - objectLength) + "//" +
                code.substring(index - objectLength);

        //System.out.println(editedCode);



        return editedCode;
    }






}
