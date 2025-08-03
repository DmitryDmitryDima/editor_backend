package com.mytry.editortry.Try.utils.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCache {

    /*
    обновляем информацию по изменениям в файле

    Пример

    Мы храним тяжелый кеш подсказок в проекте.
    Запрашивая его, мы смотрим, не были ли параллельно изменены какие-либо файлы до момента запроса
    Если да - мы пересобираем кеш подсказок
     */
    private ProjectLastUpdateInfo lastUpdate;


    // пара - id файла - кеш файла
    private final Map<Integer, FileCache> fileCashes = new HashMap<>();

    // список подписчиков - если 0. кеш проекта стирается
    private final Set<String> subscribers = new HashSet<>();










    // обновляем инфу
    public synchronized void updateInfo(ProjectLastUpdateInfo info){
        lastUpdate = info;
    }

    // получаем последний апдейт со всей информацией
    public ProjectLastUpdateInfo getLastUpdate(){return lastUpdate;}



    // вспомогательный метод для статистики / обработчика зависших кешей
    public int getSubAmount(){
        return subscribers.size();
    }


    // удаляем подписчика и возвращаем текущее число подписчиков
    public synchronized int removeSubscriber(String sessionId){
        subscribers.remove(sessionId);
        return subscribers.size();
    }

    // добавляем подписчика
    public synchronized void addSubscriber(String sessionId){
        subscribers.add(sessionId);
    }

    /*
    todo - методы добавления кеша (структура -сложная)
     */

















}
