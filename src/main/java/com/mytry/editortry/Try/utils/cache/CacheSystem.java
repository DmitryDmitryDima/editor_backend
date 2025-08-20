package com.mytry.editortry.Try.utils.cache;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


// система напрямую связана с websocket
// происходит первая  подписка на файл или проект в целом = создается кеш проекта
@Component
public class CacheSystem {

    // пара project_id : project cache
    private final Map<Long, ProjectCache> projectsCaches = new HashMap<>();


    // пара sessionId - id проекта
    private final Map<String, Long> subscribersProjectsAssosiation = new HashMap<>();





    // мы фиксируем момент последнего изменения кеша для того, чтобы специальный обработчик чистил зависшие старые кеши
    public synchronized void setProjectChange(Long projectId){
        ProjectCache projectCache = projectsCaches.get(projectId);
        if (projectCache!=null){
            projectCache.notifyUpdate();
        }
    }

    public synchronized void updateProjectCache(Long projectId,
                                                Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation,
                                                Map<Long, CacheSuggestionInnerProjectFile> idToFileAssociation){

    }

    // метод вызывается каждый раз, когда сохраняется файл. Помним, что работаем с двумя ассоциациями сразу,
    // поэтому разумнее не перезаписывать объект вообще
    public synchronized void updateFileCache(Long projectId, Long fileId, CacheSuggestionInnerProjectFile type){
        ProjectCache projectCache = projectsCaches.get(projectId);
        if (projectCache != null){
            projectCache.updateFileCache(fileId, type);
        }
    }


    // уведомляем систему о том, что в проекте произошли значительные изменения, делающие кеш неактуальным
    public synchronized void clearProjectCacheContent(Long projectId){
        ProjectCache projectCache = projectsCaches.get(projectId);
        if (projectCache!=null){
            // чистим файловый контент, но не трогаем трекер подписчиков и прочую инфу
            projectCache.clearExpiredCache();
        }
    }

    // полное стирание кеша - когда проект не имеет подписчиков
    public synchronized void removeProjectCache(Long projectId){
        projectsCaches.remove(projectId);
    }








    public synchronized void addProjectSubscription(String sessionId, Long projectId){


        ProjectCache projectCache = projectsCaches.computeIfAbsent(projectId, k -> new ProjectCache());
        subscribersProjectsAssosiation.put(sessionId, projectId);


        projectCache.addSubscriber(sessionId);


    }


    // удаляем подписчика и, если он последний, кеш
    public synchronized void removeProjectSubscription(String sessionId){

        Long projectId = subscribersProjectsAssosiation.remove(sessionId);


        ProjectCache cache = projectsCaches.get(projectId);

        if (cache!=null){
            int amount = cache.removeSubscriber(sessionId);
            if (amount==0){
                projectsCaches.remove(projectId); // очищаем кеш проекта вместе с файлами
            }

        }

    }


    public Map<Long, ProjectCache> getProjectsCaches() {
        return projectsCaches;
    }
}
