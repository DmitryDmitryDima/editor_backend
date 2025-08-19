package com.mytry.editortry.Try.utils.cache;

import com.mytry.editortry.Try.dto.basicsuggestion.CacheSuggestionInnerProjectType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCache {


    // специальный обработчик будет чистить устаревший кеш, если таковой имеется
    private Instant lastModified = Instant.now();

    // пара - id файла - кеш файла - его публичный api
    // кеш файла меняется при действиях с ним
    private Map<Long, FileCache> fileCashes = new HashMap<>();

    // список подписчиков - если 0. кеш проекта стирается
    private final Set<String> subscribers = new HashSet<>();

    /*
    ассоциации для доступа к внутреннему файловому кешу

     */

    // ассоциация - пакет = файлы
    private final Map<String, List<CacheSuggestionInnerProjectType>> packageToFileAssociation = new HashMap<>();

    // ассоциация - айди файла = файл
    private final Map<Long, CacheSuggestionInnerProjectType> idToFileAssociation = new HashMap<>();








    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

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



















}
