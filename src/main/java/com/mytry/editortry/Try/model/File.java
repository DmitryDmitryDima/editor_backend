package com.mytry.editortry.Try.model;


import jakarta.persistence.*;

@Entity
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String extension;


    // relative to file system, not absolute for specific pc
    @Column
    private String path;


    // private Project project;

    //

}
