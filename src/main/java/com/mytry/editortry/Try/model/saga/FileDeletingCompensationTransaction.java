package com.mytry.editortry.Try.model.saga;

import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long file_id;



    // путь к файлу
    private String diskPath;

    // зависший путь
    private String step;

    // количество retry'ев

    private Long attempts = 0L;


}
