package com.mytry.editortry.Try.utils.cache;

import java.time.Instant;
import java.util.*;


public class ProjectCache {


    // специальный обработчик будет чистить устаревший кеш, если таковой имеется
    private Instant lastModified = Instant.now();



    // список подписчиков - если 0. кеш проекта стирается
    private final Set<String> subscribers = new HashSet<>();

    /*
    ассоциации для доступа к внутреннему файловому кешу

     */

    // ассоциация - пакет = файлы
    private Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation;



    // ассоциация - айди файла = файл
    private Map<Long, CacheSuggestionInnerProjectFile> idToFileAssociation;





    // фиксируем последнее изменение в проекте
    public synchronized void notifyUpdate(){
        lastModified = Instant.now();
    }

    // чистим неактуальный кеш
    public synchronized void clearExpiredCache(){
        idToFileAssociation = null;
        packageToFileAssociation = null;
    }

    // вспомогательный метод, вызывая который, мы решаем, проводить ли анализ кодовой базы проекта
    public synchronized boolean isEmpty(){
        return packageToFileAssociation == null && idToFileAssociation == null;
    }



    /*
    потенциальная логика чистильщика кешей
    - если нет подписчиков - удаление
    - если есть подписчики, но последнее обновление больше некоторого времени назад - удаление
     */

    // вспомогательный метод для статистики, обработчика неактуальных кешей
    public synchronized Instant getLastUpdate() {
        return lastModified;
    }

    // вспомогательный метод для статистики / обработчика зависших кешей
    public synchronized int getSubAmount(){
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




    // геттеры и сеттеры для работы с кешированной структурой проекта

    public synchronized Map<Long, CacheSuggestionInnerProjectFile> getIdToFileAssociation() {
        return idToFileAssociation;
    }

    public synchronized void setIdToFileAssociation(Map<Long, CacheSuggestionInnerProjectFile> idToFileAssociation) {
        this.idToFileAssociation = idToFileAssociation;
    }

    public synchronized Map<String, List<CacheSuggestionInnerProjectFile>> getPackageToFileAssociation() {
        return packageToFileAssociation;
    }

    public synchronized void setPackageToFileAssociation(Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation) {
        this.packageToFileAssociation = packageToFileAssociation;
    }

    // логика точечного обновления файлового кеша с сохранением объекта
    public synchronized void updateFileCache(Long fileId, CacheSuggestionInnerProjectFile typeInfo){
        CacheSuggestionInnerProjectFile fileCache = idToFileAssociation.get(fileId);
        if (fileCache!=null){
            fileCache.updateTypeStructureFrom(typeInfo);
        }
    }
}
