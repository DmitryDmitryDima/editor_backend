package com.mytry.editortry.Try.model.saga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
Данная таблица позволяет хранить некий процесс, ассоциированный с файлом (к примеру, удаление и возможные компенсационные транзакции)
После окончания процесса последним действием запись в таблице стирается.
Уникальный код процесса равен file_id -
в нашей системе не может быть нескольких одновременно протекающих процессов по отношению к одному файлу

 */
@Entity
@Table(name = "file_idempotent_process")
@Getter
@Setter
@NoArgsConstructor
public class FileIdempotentProcess {

    @Id
    private Long id;
}
