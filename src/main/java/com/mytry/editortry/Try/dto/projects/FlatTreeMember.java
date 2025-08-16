package com.mytry.editortry.Try.dto.projects;


// DTO для react complex tree - плоская структура

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


/*
Плоское дерево следующей структуры (обязателен root)
const items = {
        root: {
            index: 'root',
            canMove: true,
            isFolder: true,
            children: ['child1', 'child2'],
            data: 'Root item',
            canRename: true,
        },
        child1: {
            index: 'child1',
            canMove: true,
            isFolder: true,
            children: [],
            data: 'Child item 1',
            canRename: true,
        },
        child2: {
            index: 'child2',
            canMove: true,
            isFolder: true,
            children: ["child3"],
            data: 'Child item 2',
            canRename: true,
        },

        child3: {
            index: 'child3',
            canMove: true,
            isFolder: false,
            children: [],
            data: 'file.java',
            canRename: true,
        }
    };
 */


/*
универсален для файлов и папок
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlatTreeMember {

    private String index; // folder + id, file + id -> все должно быть уникальным в рамках дерева
    @JsonProperty("isFolder")
    private boolean isFolder; // false для файла
    private List<String> children = new ArrayList<>(); // идентификатор в формате, используемом в index
    private String data;
    private boolean immutable; // можно переименовать все, кроме root



}
