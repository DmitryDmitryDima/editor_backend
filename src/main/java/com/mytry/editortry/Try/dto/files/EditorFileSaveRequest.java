package com.mytry.editortry.Try.dto.files;



// фронтенд посылает время последнего обновления файла на его стороне
// на стороне фронтенда время обновляется после: успешного save, при успешном чтении файла, при websocket event'ах
// есл время фронтенда раньше времени последнего обновления файла (допустим, был рассинхрон, то save не срабатывает,
// происходит принудительное обновление

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditorFileSaveRequest {

    private String content;

    private Long file_id;
    private Long project_id;





}
