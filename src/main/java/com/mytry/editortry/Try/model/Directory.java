package com.mytry.editortry.Try.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "directories")
@Getter
@Setter
@NoArgsConstructor
public class Directory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /*

    private Directory parent;   // если корень, то родителя нет - null

    private List<Directory> children; // поддиректории

    private List<File> files; // файлы в пределах директории

     */
}
