package com.mytry.editortry.Try.model.saga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_deleting_compensation_transaction")
@Getter
@Setter
@NoArgsConstructor
public class FileDeletingCompensationTransaction {
    @Id
    private Long id;

    private Long file_id;



    // путь к файлу
    private String diskPath;

    // зависший путь
    private String step;

    // количество retry'ев
    private Long attempts;


}
