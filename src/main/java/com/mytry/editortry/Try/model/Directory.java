package com.mytry.editortry.Try.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Directory> children = new ArrayList<>();

    // в корневой папке нет родителя
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private Directory parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<File> files = new ArrayList<>();


    @Column
    private Instant createdAt;

    // иммутабельность директории - соблюдение сохранения шаблона
    private boolean immutable;



    @Override
    public String toString(){
        return name;
    }
}
