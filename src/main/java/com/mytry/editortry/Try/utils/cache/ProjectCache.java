package com.mytry.editortry.Try.utils.cache;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
    потенциальная логика чистильщика кешей
    - если нет подписчиков - удаление
    - если есть подписчики, но последнее обновление больше некоторого времени назад - удаление
*/
public class ProjectCache {

    // lock для безопасных операций в многопоточной среде. Также используем стратегию defencive copy
    private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

    private final Lock readLock = globalLock.readLock();
    private final Lock writeLock = globalLock.writeLock();


    // фиксируем время последнего взаимодействия с кешем проекта
    // специальный обработчик будет искать зависшие кеши, существующие больше определенного времени, и чистить их
    private final AtomicReference<Instant> lastModified = new AtomicReference<>(Instant.now());



    // список сессий, подписанных на проект. Если ноль, кеш автоматически стирается
    private final Set<String> subscribers = ConcurrentHashMap.newKeySet();

    /*
    ассоциации для доступа к внутреннему файловому кешу
     */

    // ассоциация - пакет = файлы
    private Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation;

    // ассоциация - айди файла = файл
    private Map<Long, CacheSuggestionInnerProjectFile> idToFileAssociation;







    // фиксируем последнее изменение в проекте после определенных операций
    public void notifyUpdate(){
        lastModified.set(Instant.now());
    }



    // чистим неактуальный кеш
    public void clearExpiredCache(){

        writeLock.lock();
        try {
            idToFileAssociation.clear();
            packageToFileAssociation.clear();
        }
        finally {
            writeLock.unlock();
        }
    }

    // Проверка кеша на контент - если кеш пустой, то необходимо провести анализ проекта
    public boolean isEmpty(){

        readLock.lock();
        try {
            return packageToFileAssociation.isEmpty() && idToFileAssociation.isEmpty();
        }
        finally {
            readLock.unlock();
        }
    }
    // вспомогательный метод для статистики, обработчика неактуальных кешей
    public Instant getLastUpdate() {
        return lastModified.get();
    }

    // вспомогательный метод для статистики / обработчика зависших кешей
    public int getSubAmount(){
        return subscribers.size();
    }






    // добавляем подписчика
    public void addSubscriber(String sessionId){
        subscribers.add(sessionId);
    }

    // удаляем подписчика и возвращаем текущее число подписчиков
    public int removeSubscriber(String sessionId){
        subscribers.remove(sessionId);
        return subscribers.size();
    }






    // геттеры и сеттеры для работы с кешированной структурой проекта

    public Map<Long, CacheSuggestionInnerProjectFile> getIdToFileAssociation() {
        readLock.lock();
        try {
            return new HashMap<>(idToFileAssociation);
        }
        finally {
            readLock.unlock();
        }
    }

    public void setIdToFileAssociation(Map<Long, CacheSuggestionInnerProjectFile> idToFileAssociation) {
        writeLock.lock();
        try {
            this.idToFileAssociation = new HashMap<>(idToFileAssociation);
        }
        finally {
            writeLock.unlock();
        }

    }

    public  Map<String, List<CacheSuggestionInnerProjectFile>> getPackageToFileAssociation() {
        readLock.lock();
        try {
            return new HashMap<>(packageToFileAssociation);
        }
        finally {
            readLock.unlock();
        }


    }

    public void setPackageToFileAssociation(Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation) {
        writeLock.lock();
        try {
            this.packageToFileAssociation = new HashMap<>(packageToFileAssociation);
        }
        finally {
            writeLock.unlock();
        }


    }

    // логика точечного обновления файлового кеша с сохранением объекта
    public void updateFileCache(Long fileId, CacheSuggestionInnerProjectFile typeInfo){
        writeLock.lock();
        try {
            CacheSuggestionInnerProjectFile fileCache = idToFileAssociation.get(fileId);
            if (fileCache!=null){
                fileCache.updateTypeStructureFrom(typeInfo);
            }

        }
        finally {
            notifyUpdate();
            writeLock.unlock();
        }

    }

    // логика точечного обновления файлового кеша с сохранением объекта
    public void updateFileCache(Long fileId, CacheSuggestionInnerProjectFile typeInfo, Instant time){
        writeLock.lock();
        try {
            CacheSuggestionInnerProjectFile fileCache = idToFileAssociation.get(fileId);
            if (fileCache!=null){
                fileCache.updateTypeStructureFrom(typeInfo);
            }

        }
        finally {
            lastModified.set(time);
            writeLock.unlock();
        }

    }
}
