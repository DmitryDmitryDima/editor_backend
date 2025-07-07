package com.mytry.editortry.Try.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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






    @ManyToOne
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private Directory parent;


    @Override
    public String toString(){
        return name+"."+extension;
    }


}
