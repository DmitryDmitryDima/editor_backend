package com.mytry.editortry.Try.model;


import com.mytry.editortry.Try.model.enums.FileStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String extension;





    // статус файла
    @Enumerated(EnumType.STRING)
    private FileStatus status = FileStatus.AVAILABLE;


    // параметры времени

    @Column
    private Instant createdAt;

    @Column
    private Instant updatedAt;






    @ManyToOne
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private Directory parent;


    @Override
    public String toString(){
        return name+"."+extension;
    }


}
